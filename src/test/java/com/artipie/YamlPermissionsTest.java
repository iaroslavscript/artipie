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

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link YamlPermissions}.
 * @since 0.2
 * @checkstyle LeftCurlyCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlPermissionsTest {

    @Test
    void johnCanDownloadDeployAndDelete() throws Exception {
        final Authentication.User user = new Authentication.User("john");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "delete"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> !perm.allowed(user, "install"))
                )
            )
        );
    }

    @Test
    void janeCanDownloadAndDeploy() throws Exception {
        final Authentication.User user = new Authentication.User("jane");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> !perm.allowed(user, "install")),
                    new MatcherOf<>(perm -> !perm.allowed(user, "update"))
                )
            )
        );
    }

    @Test
    void anyoneCanDownload() throws Exception {
        MatcherAssert.assertThat(
            this.permissions().allowed(new Authentication.User("anyone"), "download"),
            new IsEqual<>(true)
        );
    }

    @Test
    void adminCanDoAnything() throws Exception {
        final Authentication.User user = new Authentication.User("admin");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "delete"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "install"); })
                )
            )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "mark,read,readers,true",
        "olga,write,group-a;group-b,true",
        "john,read,abc;def,false",
        "jane,manage,readers;leaders,false",
        "ann,read,'',false"
        }, nullValues = "''"
    )
    void checksGroups(final String name, final String action,
        final String groups, final boolean res) {
        final YamlMapping settings = new RepoPerms(
            new ListOf<>(
                new RepoPermissions.PermissionItem(name, "write"),
                new RepoPermissions.PermissionItem("/readers", "read")
            )
        ).permsYaml();
        MatcherAssert.assertThat(
            new YamlPermissions(settings).allowed(
                new Authentication.User(name, new ListOf<String>(groups.split(";"))), action
            ),
            new IsEqual<>(res)
        );
    }

    /**
     * Permissions from repo-full-config.yml example file.
     *
     * @return Permissions parsed from file.
     */
    private YamlPermissions permissions() throws IOException {
        return new YamlPermissions(
            Yaml.createYamlInput(new TestResource("repo-full-config.yml").asPath().toFile())
                .readYamlMapping()
                .yamlMapping("repo")
                .yamlMapping("permissions")
        );
    }

}
