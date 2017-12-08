#!/bin/bash

# this tests that project files (in the same directory as the call to dux)
# are properly ignored when the option is appropriately set

set -eu

cd shell

export GOOGLE_APPLICATION_CREDENTIALS=../../credentials/GOOGLE_APPLICATION_CREDENTIALS

../../bazel-bin/dux -c make

# ignore project dir set: ensure shell.c is not in the conifg
if [ `../../bazel-bin/dux -d | grep "shell.c" | wc -l` -ne 0 ]; then
    echo "Dependency was in config when ignore setting on"
    exit 1
fi

# now run including project dir
rm -f build.dux
../../bazel-bin/dux -c make -i

# now we want shell.c to be in the config
if [ `../../bazel-bin/dux -d | grep "shell.c" | wc -l` -eq 0 ]; then
    echo "Dependency was not in config when ignore setting off"
    exit 1
fi

rm -f build.dux
