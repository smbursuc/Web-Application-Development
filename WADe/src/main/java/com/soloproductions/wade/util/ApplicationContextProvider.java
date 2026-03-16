package com.soloproductions.wade.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring component that captures the {@link ApplicationContext} after initialisation
 * and exposes it statically, allowing non-managed classes to resolve beans by type.
 *
 * <p>Register it as a bean and Spring will automatically call
 * {@link #setApplicationContext(ApplicationContext)} once the context is ready.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    /** Shared reference to the active Spring application context. */
    private static ApplicationContext context;

    /**
     * Returns the active Spring application context.
     *
     * @return  the application context
     */
    public static ApplicationContext getApplicationContext() 
    {
        return context;
    }

    /**
     * Stores the application context after Spring finishes initialisation.
     *
     * @param   ac
     *          the initialised application context
     *
     * @throws  BeansException
     *          if a context-related error occurs
     */
    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException 
    {
        context = ac;
    }

    /**
     * Retrieves a bean of the given type from the application context.
     *
     * @param   <T>
     *          type of the bean
     * @param   beanClass
     *          class object identifying the desired bean
     *
     * @return  bean instance
     */
    public static <T> T getBean(Class<T> beanClass) 
    {
        return context.getBean(beanClass);
    }
}
