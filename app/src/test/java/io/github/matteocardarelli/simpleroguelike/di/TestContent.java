package io.github.matteocardarelli.simpleroguelike.di;

import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;

/** L'aspetto dei contenuti che fornisce l'app, per i test di altri package. */
public final class TestContent {

    private TestContent() {}

    public static ContentPresentation presentation() {
        return ContentModule.contentPresentation();
    }
}
