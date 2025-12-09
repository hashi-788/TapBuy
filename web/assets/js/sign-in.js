async  function signIn() {

    const user_dto = {
        email: document.getElementById("email").value,
        password: document.getElementById("password").value,
    };
    const response = await fetch(
            "SignIn",
            {
                method: "POST",
                body: JSON.stringify(user_dto),
                headers: {
                    "Content-Type": "application/json"
                }
            }
    );
    if (response.ok) {
        const json = await response.json();
        if (json.success) {
            window.location = "index.html";
        } else {
            if (json.content === "Unverified") {
                window.location = "verify-account.html";
            } else {
                document.getElementById("message").innerHTML = json.content;
            }
        }
    } else {
        document.getElementById("message").innerHTML = "Please try again later!";
    }
}

async function sendResetEmail() {
    const email = document.getElementById("email").value.trim();
    const message = document.getElementById("message");

    if (!email) {
        message.innerHTML = "Please enter your email address.";
        return;
    }

    const user_dto = { email };

    try {
        const response = await fetch("SendResetEmail", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(user_dto)
        });

        if (response.ok) {
            const json = await response.json();
            if (json.success) {
                window.location.href = "forget-password.html";
            } else {
                message.innerHTML = json.content;
            }
        } else {
            message.innerHTML = "Server error. Try again later.";
        }
    } catch (err) {
        console.error(err);
        message.innerHTML = "Network error. Try again later.";
    }
}
