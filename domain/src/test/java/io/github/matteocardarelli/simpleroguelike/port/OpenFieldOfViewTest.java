package io.github.matteocardarelli.simpleroguelike.port;

/** Il fake passa lo stesso contratto dell'adapter vero. */
class OpenFieldOfViewTest extends FieldOfViewContractTest {

    @Override
    protected FieldOfView createFieldOfView() {
        return new OpenFieldOfView();
    }
}
