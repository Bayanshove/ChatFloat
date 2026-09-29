#!/bin/bash
# Merge BubbleService_part1.java and BubbleService_part2.java into BubbleService.java
set -e

cd "$(dirname "$0")/../../app/src/main/java/com/hwcloud/chatfloat"

cat BubbleService_part1.java BubbleService_part2.java > BubbleService.java
echo "BubbleService.java merged successfully ($(wc -l < BubbleService.java) lines)"
