package lsfusion.server.physics.admin.log;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.base.controller.stack.AspectStackItem;
import lsfusion.server.physics.admin.Settings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.function.IntFunction;

import static lsfusion.server.physics.admin.log.ServerLoggers.systemLogger;

@Aspect
public class TimeLoggerAspect {

    private static long runningTotal = 0;
    private static long runningWarn = 0;
    
    @Around("execution(@lsfusion.server.physics.admin.log.LogTime * *.*(..)) && target(object)")
    public Object callMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        String methodFilter = Settings.get().getLogTimeFilter();
        boolean loggingEnabled = !methodFilter.isEmpty(); // оптимизация

        long startTime = 0;
        if (loggingEnabled)
            startTime = System.nanoTime();

        Object result = thisJoinPoint.proceed();

        if (loggingEnabled) {
            Method method = ((MethodSignature) thisJoinPoint.getSignature()).getMethod();
            if(method.getName().matches(methodFilter)) {
                long runTime = System.nanoTime() - startTime;
                runningTotal += runTime;
                
                if(runTime > Settings.get().getLogTimeThreshold() * 1000000) {
                    runningWarn += runTime;

                    ImList<String> args = AspectStackItem.getArgs(thisJoinPoint, method);
                    systemLogger.info(String.format("LogTime (%1$d ms, tot : %3$d ms, warn : %4$d ms) %2$s : " + ListFact.toList(args.size(), new IntFunction<String>() {
                        public String apply(int i) {
                            return ("%" + (5 + i) + "$s");
                        }
                    }).toString(","), ListFact.<Object>toList(runTime / 1000000, method.getName(), runningTotal / 1000000, runningWarn / 1000000).addList(args).toArray(new Object[args.size() + 4])));
                }
            }
        }

        return result;
    }
}
