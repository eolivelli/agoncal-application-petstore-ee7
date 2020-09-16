package org.agoncal.application.petstore.util;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 *         This interceptor implements Serializable because it's used on a Stateful Session Bean who has
 *         passivation and activation lifecycle.
 */

@Loggable
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class LoggingInterceptor implements Serializable
{
    private static final boolean SKIP = Boolean.getBoolean(Loggable.class.getName() + ".skip");

    // ======================================
    // =             Attributes             =
    // ======================================

    private transient volatile Logger logger;

    // ======================================
    // =          Business methods          =
    // ======================================

    @AroundInvoke
    private Object intercept(InvocationContext ic) throws Exception
    {
        if (SKIP) {
            return ic.proceed();
        }
        final String name = ic.getTarget().getClass().getName();
        if (logger == null) { // must support deserialization and contract does not
            synchronized (this) {
                if (logger == null) {
                    logger = Logger.getLogger(name);
                }
            }
        }
        logger.entering(name, ic.getMethod().getName());
        logger.info(">>> " + name + "-" + ic.getMethod().getName());
        try
        {
            return ic.proceed();
        }
        finally
        {
            logger.exiting(name, ic.getMethod().getName());
            logger.info("<<< " + name + "-" + ic.getMethod().getName());
        }
    }
}
