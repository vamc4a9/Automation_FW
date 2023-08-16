package com.qa.core.dataHandler;

import com.qa.core.context.CoreParameters;
import com.qa.core.util.BeanUtil;

public class ParserUtils {

    static synchronized String getAbsolutePathOfFile(String relativePath) {
        return BeanUtil.getBean(CoreParameters.class).getTargetFolderPath() + relativePath;
    }

}
