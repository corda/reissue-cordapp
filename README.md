<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Re-issuance CorDapp

This CorDapp contains re-issuance flows which eliminate the risk of being left without a usable state if issuer does not reissue.

1. Party (requester) creates re-issuance request and sends it to the issuer
1. Issuer consumes the request and generates an encumbered copy of state provided in the request and a lock state which enforces successful re-issuance
1. Requester deletes the original state
1. Requester uploads transaction generated in the previous step as an attachment
1. Requester encumbers the new state providing the lock object and the created attachment
