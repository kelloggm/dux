java_binary(
    name = "dux",
    main_class = "org.dux.cli.DuxCLI",
    deps = ["@google_options//:compile", 
            "@checker_qual//:compile",
            "@google_cloud_storage//:compile",
            "@slf4j//:compile",
            "@logback_classic//:compile",
            "@jna//:compile",
            "@jna_platform//:compile"],
    srcs = glob(["src/org/dux/cli/*.java", 
                 "src/org/dux/backingstore/*.java",
		 "src/org/dux/stracetool/*.java"]),
)
