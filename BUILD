java_binary(
    name = "dux",
    main_class = "org.dux.cli.DuxCLI",
    deps = ["@google_options//:compile", 
            "@checker_qual//:compile",
            "@google_cloud_storage//:compile",
            "@slf4j//:compile",
            "@logback_classic//:compile"],
    srcs = glob(["src/org/dux/cli/*.java", 
                 "src/org/dux/backingstore/*.java"]),
)

sh_test(
    name = "dux_smoke",
    size = "small",
    srcs = ["tst/smoke.sh"],
    data = glob(["tst/shell/*"]),
)

sh_test(
    name = "dux_smoke_config",
    size = "small",
    srcs = ["tst/smoke_config.sh"],
    data = glob(["tst/shell/*"]),
)