java_binary(
    name = "dux",
    main_class = "org.dux.cli.DuxCLI",
    deps = ["@com_github_pcj_google_options//jar", 
            "@org_checkerframework_checker_qual//jar",
            "@com_google_guava_guava//jar"],
    srcs = glob(["src/org/dux/cli/*.java"]),
    jvm_flags = ["-DGOOGLE_APPLICATION_CREDENTIALS=$(workspace)/credentials/duxserver-74f9ff868003.json"],
)