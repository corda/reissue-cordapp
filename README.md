<p align="center">	
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">	
</p>

# Purpose of the re-issuance CorDapp

When creating a new transaction, input states are included in the proposed transaction by reference.
These input state references link transactions together over time, forming a transaction back-chain.

Long transaction back-chains are undesirable for two reasons:
- performance - resolution along the chain slows down significantly
- privacy - all back-chain transactions are shared with the new owner

The known approach to resolve the problem with long transaction chains is state re-issuance.
In the case where the issuer of an asset is trustworthy, the state can simply be exited from the ledger and then re-issued. 
As there are no links between the exit and re-issuance transactions, it prunes the back-chain. 

As the actions described above are not atomic, it is possible for the issuer not to re-issue the state. 
To eliminate that risk, the slightly modified approach is used in the CorDapp. The idea behind the CorDapp is for 
an issuer to re-issue encumbered (locked) state before the original state is deleted and allow the requester to 
unlock re-issued state immediately after the original state is deleted.

# Installation of Re-issuance CorDapp

Re-issuance CorDapp can be installed as any other CorDapp. 

Firstly, artifactory repository containing re-issuance CorDapp (`corda-lib-dev`) needs to be added to the list 
of repositories for the project:
```
repositories {
    maven { url 'https://ci-artifactory.corda.r3cev.com/artifactory/corda-lib-dev' }
}
```

Secondly, dependencies must be added to the `dependencies` block in each module using the re-issuance CorDapp:
```
cordaCompile "$tokens_release_group:tokens-contracts:$tokens_release_version"
cordaCompile "$tokens_release_group:tokens-contracts:$tokens_release_version"
```

`tokens_release_group` and `tokens_release_version` are variables which should be included in `gradle.properties` file.

# Re-issuance flows
## Requesting re-issuance
`RequestReIssuanceAndShareRequiredTransactions` flow is used to request re-issuance and share required transaction with 
an issuer.

Parameters:
* `issuer: AbstractParty`
* `stateRefsToReIssue: List<StateRef>`
* `assetIssuanceCommand: CommandData` - command which is supposed to be used to issue the new asset
* `extraAssetIssuanceSigners: List<AbstractParty>` - required issuance signers other than issuer, empty list by default
* `requester: AbstractParty?` - needs to be provided only when requester is an account, should be null otherwise 
which is a default value

## Accept re-issuance request
`ReIssueStates` is used to accept re-issuance request, create a locked copy of the original states, and a re-issuance 
lock object which enforces successful re-issuance and is used to prevent cheating.

Parameters:
* `reIssuanceRequestStateAndRef: StateAndRef<ReIssuanceRequest>`
* `issuerIsRequiredExitCommandSigner: Boolean` - determines whether issuer signature is checked in asset 
state exit transaction verification (see [Requester unlocks re-issued states](#requester-unlocks-re-issued-states),
true by default

## Reject re-issuance request
`RejectReIssuanceRequest` is used to reject re-issuance request.

Parameters:
* `reIssuanceRequestStateAndRef: StateAndRef<ReIssuanceRequest>`

## Transform transaction into byte array
`GenerateTransactionByteArray` is used to transform a transaction into a byte array to be able to add it to another 
transaction as an attachment.

Parameters:
* `transactionId: SecureHash`

## Unlock re-issued states
`UnlockReIssuedStates` is used to unlock re-issued states after the original states had been exited from the ledger.

Parameters:
* `reIssuedStateAndRefs: List<StateAndRef<T>>`
* `reIssuanceLock: StateAndRef<ReIssuanceLock<T>>`
* `deletedStateTransactionHashes: List<SecureHash>`
* `assetUpdateCommand: CommandData` - command which should be used to unencumber asset states
* `extraAssetUpdateSigners: List<AbstractParty>` - required update command signers other than requester 
(empty list be default)

## Exit re-issued states from the ledger
`DeleteReIssuedStatesAndLock` is used to exit re-issued states and corresponding re-issuance lock object if they are 
no longer needed (when the original states had been consumed and the re-issued states will never able to be re-issued).

Parameters:
* `reIssuanceLockStateAndRef: StateAndRef<ReIssuanceLock<T>>`
* `reIssuedStateAndRefs: List<StateAndRef<T>>`
* `assetExitCommand: CommandData` - command which should be used to exit encumbered asset states
* `assetExitSigners: List<AbstractParty>` - `assetExitCommand` signers, by default a list of issuer and requester 

# Useful inks

Re-issuance CorDapp design: <!-- TODO: insert link once it's merged into master branch -->

Sample CorDapp: https://github.com/corda/reissue-sample-cordapp
