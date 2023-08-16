package com.qa.core.basePage;

import com.qa.core.util.BeanUtil;
import com.qa.core.context.CoreParameters;
import com.qa.core.web.UiLib;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A factory for creating PageInstances objects.
 */
@Component
@Lazy
@Scope("prototype")
public class PageProvider {

    private final UiLib uilib;

    public PageProvider(UiLib uilib) {
        this.uilib = uilib;
    }
    /**
     * Gets the single instance of PageInstanceFactory.
     *
     * @param <T>  the generic type
     * @param type the type
     * @return single instance of PageInstanceFactory
     */
    public <T extends CoreBasePage> T getPage(Class<T> type) {
        try {
            T newInstance = BeanUtil.getBean(type);
            CoreParameters.setPage(newInstance.getClass());
            uilib.waitForPageLoad(newInstance.getClass());
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting page instance of " + type.getName());
        }
    }

}
