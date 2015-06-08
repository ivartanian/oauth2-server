package uk.co.caeldev.oauthserver.builders;

import uk.co.caeldev.oauthserver.persisters.Persister;

public interface Builder<T> {

    T build();

    T persist(Persister persister);
}
