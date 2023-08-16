package com.qa.core.util;

import com.qa.core.context.CoreParameters;
import com.qa.core.context.RunConfiguration;
import com.qa.core.report.ReportManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.text.diff.CommandVisitor;
import org.apache.commons.text.diff.StringsComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

@Component
@Lazy
@Scope("prototype")
public class FileComparator {

    public boolean failFlag = false;
    public boolean fileSizeIssue = false;
    private String file1Path;
    private String file2Path;

    private FileComparator() {}

    public void init(String file1Path, String file2Path) {
        this.file1Path = file1Path;
        this.file2Path = file2Path;
    }

    private double getFileSizeKiloBytes(File file) {
        return (double) file.length() / 1024;
    }

    public synchronized String compare() throws IOException {
        // Read both files with line iterator.
        double sourceSize = getFileSizeKiloBytes(new File(file1Path));
        double targetSize = getFileSizeKiloBytes(new File(file2Path));

        if (Math.abs(sourceSize - targetSize) < 10) {
            LineIterator file1 = FileUtils.lineIterator(new File(file1Path), "utf-8");
            LineIterator file2 = FileUtils.lineIterator(new File(file2Path), "utf-8");

            String file1Data = FileUtils.readFileToString(new File(file1Path), "utf-8");
            String file2Data = FileUtils.readFileToString(new File(file2Path), "utf-8");

            // Initialize visitor.
            FileCommandsVisitor fileCommandsVisitor = BeanUtil.getBean(FileCommandsVisitor.class);
            if (!file1Data.contentEquals(file2Data)) {
                // Read file line by line so that comparison can be done line by line.
                while (file1.hasNext() || file2.hasNext()) {
                    /*
                     * In case both files have different number of lines, fill in with empty
                     * strings. Also append newline char at end so next line comparison moves to
                     * next line.
                     */
                    String left = (file1.hasNext() ? file1.nextLine() : "") + "\n";
                    String right = (file2.hasNext() ? file2.nextLine() : "") + "\n";
                    if ((left.contains("_QT") || left.contains("\"NEW\"") || right.contains("_QT")
                            || left.contains("UPDATED_AT") || right.contains("UPDATED_AT"))) {
                    } else {

                        if (left.contains("\"PERIOD\": "))
                            left = left.toLowerCase();

                        if (right.contains("\"PERIOD\": "))
                            right = right.toLowerCase();

                        // Prepare diff comparator with lines from both files.
                        StringsComparator comparator = new StringsComparator(left, right);

                        if (comparator.getScript().getLCSLength() != Integer.max(left.length(), right.length()))
                            failFlag = true;

                        if (comparator.getScript().getLCSLength() > (Integer.max(left.length(), right.length()) * 0.4)) {
                            /*
                             * If both lines have atleast 40% commonality then only compare with each other
                             * so that they are aligned with each other in final diff HTML.
                             */
                            comparator.getScript().visit(fileCommandsVisitor);
                        } else {
                            /*
                             * If both lines do not have 40% commanlity then compare each with empty line so
                             * that they are not aligned to each other in final diff instead they show up on
                             * separate lines.
                             */
                            StringsComparator leftComparator = new StringsComparator(left, "\n");
                            leftComparator.getScript().visit(fileCommandsVisitor);
                            StringsComparator rightComparator = new StringsComparator("\n", right);
                            rightComparator.getScript().visit(fileCommandsVisitor);
                        }
                    }
                }
                return fileCommandsVisitor.generateHTML();
            } else {
                return "pass";
            }
        }

        fileSizeIssue = true;
        return "source and target files are of two different sizes, please compare them manually";

    }
}

/*
 * Custom visitor for file comparison which stores comparison & also generates
 * HTML in the end.
 */
@Component
@Lazy
@Scope("prototype")
class FileCommandsVisitor implements CommandVisitor<Character> {

    @Autowired
    RunConfiguration config;

    @Autowired
    CoreParameters coreParameters;

    private FileCommandsVisitor() {}

    // Spans with red & green highlights to put highlighted characters in HTML
    private String DELETION = "<span style=\"background-color: #FB504B\">${text}</span>";
    private String INSERTION = "<span style=\"background-color: #45EA85\">${text}</span>";

    private String left = "";
    private String right = "";

    @Override
    public void visitKeepCommand(Character c) {
        // For new line use <br/> so that in HTML also it shows on next line.
        String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
        // KeepCommand means c present in both left & right. So add this to both without
        // any
        // highlight.
        left = left + toAppend;
        right = right + toAppend;
    }

    @Override
    public void visitInsertCommand(Character c) {
        // For new line use <br/> so that in HTML also it shows on next line.
        String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
        // InsertCommand means character is present in right file but not in left. Show
        // with green highlight on right.
        right = right + INSERTION.replace("${text}", "" + toAppend);
    }

    @Override
    public void visitDeleteCommand(Character c) {
        // For new line use <br/> so that in HTML also it shows on next line.
        String toAppend = "\n".equals("" + c) ? "<br/>" : "" + c;
        // DeleteCommand means character is present in left file but not in right. Show
        // with red highlight on left.
        left = left + DELETION.replace("${text}", "" + toAppend);
    }

    public String generateHTML() throws IOException {
        String[] filePaths = getFinalHtmlFilePath("diff");
        // Get template & replace placeholders with left & right variables with actual
        // comparison
        String template = FileUtils.readFileToString(new File(coreParameters.getTargetFolderPath() +
                config.getProperty("file_difference_html_template")), "utf-8");
        String out1 = template.replace("${left}", left);
        String output = out1.replace("${right}", right);
        // Write file to disk.
        FileUtils.write(new File(filePaths[1]), output, "utf-8");
        return filePaths[0];
    }

    private String[] getFinalHtmlFilePath(String name) {
        String file_name = name.replace(" ", "_") + "_" + Calendar.getInstance().getTimeInMillis() + ".html";
        String absPath = ReportManager.screenshotsFolder + file_name;
        String relativePath = absPath.replace(ReportManager.resultsFolder, "./");
        return new String[]{relativePath, absPath};
    }
}
