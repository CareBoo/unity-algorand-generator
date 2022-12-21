#!/usr/bin/env bash

curl https://raw.githubusercontent.com/algorand/go-algorand/rel/stable/daemon/algod/api/algod.oas2.json --output resources/algod/algod.oas2.json
curl https://raw.githubusercontent.com/algorand/indexer/master/api/indexer.oas2.json --output resources/indexer/indexer.oas2.json
# curl https://raw.githubusercontent.com/algorand/go-algorand/rel/stable/daemon/kmd/api/swagger.json --output resources/kmd/swagger.json
