/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.fs.RxFile;
import io.vertx.reactivex.core.Vertx;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RepoConfig}.
 * @since 0.2
 */
public final class RepoConfigTest {

    /**
     * Vertx instance.
     */
    private Vertx vertx;

    @Test
    public void readsCustom() throws Exception {
        final RepoConfig config = this.readFull();
        final YamlMapping yaml = config.settings().orElseThrow();
        MatcherAssert.assertThat(
            yaml.string("custom-property"),
            new IsEqual<>("custom-value")
        );
    }

    @Test
    public void failsToReadCustom() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            "Unexpected custom config",
            config.settings().isEmpty()
        );
    }

    @Test
    public void readContentLengthMax() throws Exception {
        final RepoConfig config = this.readFull();
        final long value = 123L;
        MatcherAssert.assertThat(
            config.contentLengthMax(),
            new IsEqual<>(Optional.of(value))
        );
    }

    @Test
    public void readEmptyContentLengthMax() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            config.contentLengthMax().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    public void readsPortWhenSpecified() throws Exception {
        final RepoConfig config = this.readFull();
        final int expected = 1234;
        MatcherAssert.assertThat(
            config.port(),
            new IsEqual<>(OptionalInt.of(expected))
        );
    }

    @Test
    public void readsEmptyPortWhenNotSpecified() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            config.port(),
            new IsEqual<>(OptionalInt.empty())
        );
    }

    @BeforeEach
    void setUp() {
        this.vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        this.vertx.close();
    }

    private RepoConfig readFull() throws Exception {
        return this.readFromResource("repo-full-config.yml");
    }

    private RepoConfig readMin() throws Exception {
        return this.readFromResource("repo-min-config.yml");
    }

    private RepoConfig readFromResource(final String name)
        throws URISyntaxException, ExecutionException, InterruptedException {
        final RxFile file = new RxFile(
            Paths.get(
                Thread.currentThread()
                    .getContextClassLoader()
                    .getResource(name)
                    .toURI()
            )
        );
        return RepoConfig.fromPublisher(StorageAliases.EMPTY, new Key.From(name), file.flow())
            .toCompletableFuture().get();
    }
}
