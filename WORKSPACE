git_repository(
  name = "org_pubref_rules_maven",
  remote = "https://github.com/pubref/rules_maven",
  commit = "9c3b07a6d9b195a1192aea3cd78afd1f66c80710",
)

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repositories")
maven_repositories()

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repository")

maven_repository(
    name = 'google_options',
    deps = [
        'com.github.pcj:google-options:1.0.0',
    ],
    transitive_deps = [
        '85d54fe6771e5ff0d54827b0a3315c3e12fdd0c7:com.github.pcj:google-options:1.0.0',
        'f7be08ec23c21485b9b5a1cf1654c2ec8c58168d:com.google.code.findbugs:jsr305:3.0.1',
        '89507701249388e1ed5ddcf8c41f4ce1be7831ef:com.google.guava:guava:20.0',
    ],
)

load("@google_options//:rules.bzl", "google_options_compile")
google_options_compile()

maven_repository(
    name = 'checker_qual',
    deps = [
        'org.checkerframework:checker-qual:2.2.2',
    ],
    transitive_deps = [
        '5539e45d0db370ad094c76a3541eb1ccb911ff7b:org.checkerframework:checker-qual:2.2.2',
    ],
)

load("@checker_qual//:rules.bzl", "checker_qual_compile")
checker_qual_compile()

maven_repository(
    name = 'google_cloud_storage',
    deps = [
        'com.google.cloud:google-cloud-storage:1.12.0',
    ],
    force = [
        'com.google.code.findbugs:jsr305:3.0.0',
        'com.google.guava:guava:20.0',
        'com.google.protobuf:protobuf-java:3.4.0',
        'com.google.api:api-common:1.2.0',
        'com.google.http-client:google-http-client:1.23.0',
        'com.google.http-client:google-http-client-jackson2:1.23.0',
    ],
    transitive_deps = [
        'f6c3aed1cdfa21b5c1737c915186ea93a95a58bd:com.fasterxml.jackson.core:jackson-core:2.1.3',
        'ac251a3623e19c4eb0a7dbb503ca32a0515f9713:com.google.api:api-common:1.2.0',
        '522ea860eb48dee71dfe2c61a1fd09663539f556:com.google.api-client:google-api-client:1.23.0',
        'b719f1b3ffac7d67ce898441f8c91fc5060b27cf:com.google.api:gax:1.15.0',
        'de4e859c3530f7e9f854e40b0a8b7074d95e3aff:com.google.api.grpc:proto-google-common-protos:1.0.1',
        'f9898e5e114e422f5eb6ea22c9102f6095253956:com.google.api.grpc:proto-google-iam-v1:0.1.25',
        '53c10dbf25674f3877a2784b219763b75fe484be:com.google.apis:google-api-services-storage:v1-rev114-1.23.0',
        '8e2b181feff6005c9cbc6f5c1c1e2d3ec9138d46:com.google.auth:google-auth-library-credentials:0.9.0',
        '04e6152c3aead24148627e84f5651e79698c00d9:com.google.auth:google-auth-library-oauth2-http:0.9.0',
        '6873fed014fe1de1051aae2af68ba266d2934471:com.google.auto.value:auto-value:1.2',
        'cc918b2fcfab7789543d632bd5e1b8c2a067fbb4:com.google.cloud:google-cloud-core:1.12.0',
        'e458fb085808e236f4ced89902f6236737c22e25:com.google.cloud:google-cloud-core-http:1.12.0',
        '617caf1b34d49988dd5977e26002ff2f705b2b41:com.google.cloud:google-cloud-storage:1.12.0',
        'f7be08ec23c21485b9b5a1cf1654c2ec8c58168d:com.google.code.findbugs:jsr305:3.0.1',
        '751f548c85fa49f330cecbb1875893f971b33c4e:com.google.code.gson:gson:2.7',
        '89507701249388e1ed5ddcf8c41f4ce1be7831ef:com.google.guava:guava:20.0',
        '8e86c84ff3c98eca6423e97780325b299133d858:com.google.http-client:google-http-client:1.23.0',
        '0eda0d0f758c1cc525866e52e1226c4eb579d130:com.google.http-client:google-http-client-appengine:1.23.0',
        'a72ea3a197937ef63a893e73df312dac0d813663:com.google.http-client:google-http-client-jackson:1.23.0',
        'fd6761f4046a8cb0455e6fa5f58e12b061e9826e:com.google.http-client:google-http-client-jackson2:1.23.0',
        'e57ea1e2220bda5a2bd24ff17860212861f3c5cf:com.google.oauth-client:google-oauth-client:1.23.0',
        'b32aba0cbe737a4ca953f71688725972e3ee927c:com.google.protobuf:protobuf-java:3.4.0',
        '96aba8ab71c16018c6adf66771ce15c6491bc0fe:com.google.protobuf:protobuf-java-util:3.4.0',
        'fd32786786e2adb664d5ecc965da47629dca14ba:commons-codec:commons-codec:1.3',
        '5043bfebc3db072ed80fbd362e7caf00e885d8ae:commons-logging:commons-logging:1.1.1',
        '36d6e77a419cb455e6fd5909f6f96b168e21e9d0:joda-time:joda-time:2.9.2',
        '1d7d28fa738bdbfe4fbd895d9486308999bdf440:org.apache.httpcomponents:httpclient:4.0.1',
        'e813b8722c387b22e1adccf7914729db09bcb4a9:org.apache.httpcomponents:httpcore:4.0.1',
        'e32303ef8bd18a5c9272780d49b81c95e05ddf43:org.codehaus.jackson:jackson-core-asl:1.9.11',
        'aca5eb39e2a12fddd6c472b240afe9ebea3a6733:org.json:json:20160810',
        '3ea31c96676ff12ab56be0b1af6fff61d1a4f1f2:org.threeten:threetenbp:1.3.3',
    ],
)

load("@google_cloud_storage//:rules.bzl", "google_cloud_storage_compile")
google_cloud_storage_compile()
