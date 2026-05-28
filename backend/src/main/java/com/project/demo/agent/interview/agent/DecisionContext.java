package com.project.demo.agent.interview.agent;

/**
 * ThreadLocal holder for DecisionAgent tool call routing.
 * Each {@code @Tool} method sets the route name before returning.
 */
public class DecisionContext {
    private static final ThreadLocal<String> LAST_ROUTE = new ThreadLocal<>();

    public static void setLastRoute(String route) {
        LAST_ROUTE.set(route);
    }

    public static String getLastRoute() {
        return LAST_ROUTE.get();
    }

    public static void clear() {
        LAST_ROUTE.remove();
    }
}
