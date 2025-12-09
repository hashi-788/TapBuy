async function forgetPassword() {
    const newPassword = document.getElementById("new-password").value;
    const confirmPassword = document.getElementById("confirm-password").value;
    const verificationCode = document.getElementById("rest-verification").value;
    const messageElem = document.getElementById("message");

    const forget_dto = {
        password: newPassword,
        confirmPassword: confirmPassword,
        verificationCode: verificationCode
    };

    try {
        console.log("Sending password reset request...");

        const response = await fetch("ForgetPassword", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(forget_dto),
            credentials: "include" // 🔥 VERY IMPORTANT for session cookies
        });

        console.log("Response status:", response.status);

        if (!response.ok) {
            messageElem.innerHTML = "Server not responding. Try again.";
            return;
        }

        const json = await response.json();
        console.log("Response JSON:", json);

        if (json.success) {
            messageElem.style.color = "green";
            messageElem.innerHTML = json.content;

            // redirect after success
            setTimeout(() => window.location = "sign-in.html", 2000);
        } else {
            messageElem.style.color = "red";
            messageElem.innerHTML = json.content;
        }

    } catch (err) {
        console.error("Error while resetting password:", err);
        messageElem.innerHTML = "Something went wrong. Please try again later.";
    }
}
