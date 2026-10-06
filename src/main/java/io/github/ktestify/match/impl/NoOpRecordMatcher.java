/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ktestify.match.impl;

import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.match.RecordMatcher;
import io.github.ktestify.models.ConsumedRecord;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@link RecordMatcher} that always passes.
 *
 * <p>Use this when the test only needs to verify that a record was <em>produced</em> to the source (i.e. it exists),
 * without asserting anything about its content. It is also the safe default when no {@code matchMethod} has been
 * configured.
 *
 * @param <V> the type of the record value
 * @since 0.3.0
 */
@Slf4j
public class NoOpRecordMatcher<V> implements RecordMatcher<V> {

    @Override
    public MatchResult match(List<ConsumedRecord<V>> records, MatchContext context) {
        log.debug(
                "NoOpRecordMatcher: skipping assertion on {} record(s) — no match method configured.", records.size());
        return MatchResult.pass();
    }
}
