package com.aion.host.automation;

// Runs in the privileged Shizuku process; kept to one method on purpose (T-043).
interface IShellUserService {
    String exec(String command);
}
