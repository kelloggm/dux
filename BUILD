java_binary(
    name = "dux",
    main_class = "org.dux.cli.DuxCLI",
    deps = ["@google_options//:compile", 
            "@checker_qual//:compile",
            "@google_cloud_storage//:compile"],
    srcs = glob(["src/org/dux/cli/*.java", 
                 "src/org/dux/backingstore/*.java"]),
)