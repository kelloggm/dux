#!/bin/bash

# this tests that the config checking behavior works,
# i.e., that if there's a missing dependency, dux will
# pull it in

set -eu

cd shell

export GOOGLE_APPLICATION_CREDENTIALS=../../credentials/GOOGLE_APPLICATION_CREDENTIALS

# include project files in the config this time
../../bazel-bin/dux -c make -i

# redirecting stderr to stdout for grepping
if [ `../../bazel-bin/dux -k 2>&1 | grep "does not exist\|does not match" | wc -l` -ne 0 ]; then
    echo "Checker incorrectly noted missing or incorrect dependencies"
    ../../bazel-bin/dux -k | grep "goes not exist\|does not match"
    exit 1
fi

# remove a dependency and see that it is pulled back
mv shell.c ../shell.c

if [ `../../bazel-bin/dux -k 2>&1 | grep "does not exist" | wc -l` -eq 0 ]; then
    echo "Checker failed to catch a missing dependency"
    ../../bazel-bin/dux -k
    exit 1
fi

if ! diff shell.c ../shell.c; then
    echo "Failed to pull missing dependency back"
    diff shell.c ../shell.c
    rm -f shell.c
    mv ../shell.c shell.c
    exit 1
fi

rm -f ../shell.c
rm -f build.dux
