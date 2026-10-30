package io.github.matteocardarelli.simpleroguelike.ui.game;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Sulla JVM non c'è il thread principale di Android: LiveData esegue tutto subito, nel thread del
 * test. È l'equivalente per JUnit 5 della regola di JUnit 4 di AndroidX.
 */
final class InstantTaskExecutorExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        ArchTaskExecutor.getInstance()
                .setDelegate(
                        new TaskExecutor() {
                            @Override
                            public void executeOnDiskIO(Runnable runnable) {
                                runnable.run();
                            }

                            @Override
                            public void postToMainThread(Runnable runnable) {
                                runnable.run();
                            }

                            @Override
                            public boolean isMainThread() {
                                return true;
                            }
                        });
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }
}
