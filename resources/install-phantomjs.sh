#!/usr/bin/env bash
set -x

PHANTOMJS_PATH="$(command -v phantomjs)"

if [ -x "$PHANTOMJS_PATH" ]; then
    echo "PhantomJS found at ${PHANTOMJS_PATH}.";
else
    echo "Installing PhantomJS...";

    PHANTOMJS_VERSION=2.1.1
    curl -L -O "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-${PHANTOMJS_VERSION}-linux-x86_64.tar.bz2";
    tar vxjf "phantomjs-${PHANTOMJS_VERSION}-linux-x86_64.tar.bz2"
    mv "phantomjs-${PHANTOMJS_VERSION}-linux-x86_64" phantomjs;

    PHANTOMJS_PATH="$(command -v phantomjs)";
    echo "PhantomJS installed at ${PHANTOMJS_PATH}.";
fi
