#!/bin/bash

set -e
shopt -s nullglob

# For any new directory in /opt/arch/in/collections that is not yet
# represented as being authorized for the built-in test user, authorize it.
for dir in /opt/arch/shared/in/collections/*; do
    collection_key=`basename $dir`;
    collection_name=`echo $collection_key | sed -r 's/(^|-)(\w)/ \U\2/g' | sed 's/^ //'`;
    cat <<< $(jq ".collections |= if has(\"$collection_key\") then . else  .\"$collection_key\" = {name: \"$collection_name\", path: \"$dir\"} end | .users[\"arch:test\"] |= (.+ [\"$collection_key\"] | unique)" /opt/arch/data/special-collections.json) > /opt/arch/data/special-collections.json
done

exec "$@"
