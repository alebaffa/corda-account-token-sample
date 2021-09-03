package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService

@StartableByRPC
@StartableByService
@InitiatingFlow
class CreateNewAccountFlow(private val accountName:String) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        subFlow(CreateAccount(accountName))
        return "$accountName account has been created"
    }
}
