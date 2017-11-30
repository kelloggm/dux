java_binary(
    name = "dux",
    main_class = "org.dux.cli.DuxCLI",
    deps = ["@com_github_pcj_google_options//jar", 
            "@org_checkerframework_checker_qual//jar",
            "@com_google_guava_guava//jar",
            "@com_google_cloud_storage//jar",
            "@com_google_cloud_core//jar"],
    srcs = glob(["src/org/dux/cli/*.java", 
                 "src/org/dux/backingstore/*.java"]),
)