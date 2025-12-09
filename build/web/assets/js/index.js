async function checkSignIn() {

    const response = await fetch("CheckSignIn");

    if (response.ok) {

        const json = await response.json();

        const response_dto = json.response_dto;

        if (response_dto.success) {
            // signed in
            const user = response_dto.content;

            let st_button_1 = document.getElementById("st-button-1");
            st_button_1.href = "SignOut";
            st_button_1.innerHTML = "Sign Out";

        } else {
            //  not signed in
            console.log("not signed in");
        }
    }
}

async function viewCart() {

    const response = await fetch("cart.html");
    if (response.ok) {
        const cartHtmlText = await response.text();
        document.querySelector("#index-main").innerHTML = cartHtmlText;
        loadCartItems();

        console.log("Loaded cart.html content:", cartHtmlText);
    } else {
        console.error("Failed to load cart.html:", response.status);
    }
}

