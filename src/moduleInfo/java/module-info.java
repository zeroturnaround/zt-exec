/*
 * Copyright (C) 2013 ZeroTurnaround <support@zeroturnaround.com>
 * Copyright (C) 2026 Neeme Praks
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// slf4j-api 1.7.x has no module descriptor, so org.slf4j is an automatic module.
// `requires transitive` on it is intentional (see below) and the usual caveat — an
// unstable derived module name — does not apply: slf4j-api pins
// `Automatic-Module-Name: org.slf4j`, which is also slf4j 2.x's real module name.
@SuppressWarnings({"requires-transitive-automatic", "requires-automatic"})
module org.zeroturnaround.exec {
  // slf4j types appear in the public API (e.g. ProcessExecutor.info(org.slf4j.Logger)),
  // so consumers reading this module also read org.slf4j.
  requires transitive org.slf4j;

  exports org.zeroturnaround.exec;
  exports org.zeroturnaround.exec.close;
  exports org.zeroturnaround.exec.listener;
  exports org.zeroturnaround.exec.stop;
  exports org.zeroturnaround.exec.stream;
  exports org.zeroturnaround.exec.stream.slf4j;
}
