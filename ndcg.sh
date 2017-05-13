#!/usr/bin/env sh
# ./ndcg.sh <rankFile relFile outputFile> taskType
java -Xmx1024m -cp classes edu.stanford.cs276.NdcgMain $1 $2 $3
