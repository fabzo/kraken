package fabzo.kraken.handler;

import fabzo.kraken.EnvironmentContext;
import fabzo.kraken.components.ComponentState;
import fabzo.kraken.components.InfrastructureComponent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public abstract class AbstractLifecycleHandler implements LifecycleHandler {
    private boolean isInitialized = false;

    @Override
    public boolean run(final InfrastructureComponent component, final EnvironmentContext ctx) {
        final boolean runResult = doRun(component, ctx);

        waitForComponent(component, ctx);

        return runResult;
    }

    protected void waitForComponent(final InfrastructureComponent component, final EnvironmentContext ctx) {
        component.waitFuncs().forEach(wait -> {
            log.info("Waiting for {} using {}", component.name(), wait.toString());
            if (!wait.execute(ctx, component)) {
                throw new IllegalStateException("Wait function " + wait.getClass().getSimpleName() + " failed");
            }
        });
    }

    protected boolean isRunning(final InfrastructureComponent component) {
        val state = component.state();

        return state == ComponentState.CREATING
                || state == ComponentState.STARTING
                || state == ComponentState.STARTED;
    }

    protected boolean isStopped(final InfrastructureComponent component) {
        val state = component.state();

        return state == ComponentState.STOPPED;
    }

    protected abstract boolean doRun(final InfrastructureComponent component, final EnvironmentContext ctx);
}
