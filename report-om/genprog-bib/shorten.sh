#!/bin/sh
# for shortened bibtex entries, pipe your .bbl through this script
# (eventually this should be a makefile option)

SED_CMD=""
REPLACEMENTS="$(cat <<EOF
Transaction   Trans
transaction   trans
Software      Soft
software      soft
Language      Lang
language      lang
International Intl
international intl
Engineering   Eng
engineering   eng
Conference    Conf
conference    conf
EOF
)"

## build up the sed command
IFS="
"
for pair in $REPLACEMENTS;do
    from=$(echo $pair|awk '{print $1}')
    to=$(echo $pair|awk '{print $2}')
    SED_CMD="$SED_CMD;s/$from/$to./g"
done

echo "$SED_CMD"
cat -|sed "$SED_CMD"
