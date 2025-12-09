const popup = Notification();

// Payment completed. It can be a successful failure.
payhere.onCompleted = function onCompleted(orderId) {
    console.log("Payment completed. OrderID:" + orderId);
    // Note: validate the payment and show success or failure page to the customer
    popup.success({
        message: "Thank you, Payment completed!"
    });
    window.location = "index.html";
};

// Payment window closed
payhere.onDismissed = function onDismissed() {
    // Note: Prompt user to pay again or show an error page
    console.log("Payment dismissed");
};

// Error occurred
payhere.onError = function onError(error) {
    // Note: show an error page
    console.log("Error:" + error);
};

async function loadData() {

    const response = await fetch("LoadCheckout");

    if (response.ok) {
        const json = await response.json();
        console.log(json);

        if (json.success) {

            const address = json.address;   // might be NULL
            const cityList = json.cityList;
            const cartList = json.cartList;

            // load cities
            let citySelect = document.getElementById("city");
            citySelect.length = 1;

            cityList.forEach(city => {
                let option = document.createElement("option");
                option.value = city.id;
                option.innerHTML = city.name;
                citySelect.appendChild(option);
            });

            // --- FIX: safe loading of address -------------------------
            let currentAddressCheckbox = document.getElementById("checkbox1");
            currentAddressCheckbox.addEventListener("change", e => {

                let firstName = document.getElementById("first-name");
                let lastName = document.getElementById("last-name");
                let city = document.getElementById("city");
                let address1 = document.getElementById("address1");
                let address2 = document.getElementById("address2");
                let postalCode = document.getElementById("postal-code");
                let mobile = document.getElementById("mobile");

                if (currentAddressCheckbox.checked) {

                    if (address == null) {
                        popup.info({ message: "No previous address found" });
                        currentAddressCheckbox.checked = false;
                        return;
                    }

                    firstName.value = address.first_name;
                    lastName.value = address.last_name;
                    city.value = address.city.id;
                    city.disabled = true;
                    city.dispatchEvent(new Event("change"));
                    address1.value = address.line1;
                    address2.value = address.line2;
                    postalCode.value = address.postal_code;
                    mobile.value = address.mobile;

                } else {
                    firstName.value = "";
                    lastName.value = "";
                    city.value = 0;
                    city.disabled = false;
                    city.dispatchEvent(new Event("change"));
                    address1.value = "";
                    address2.value = "";
                    postalCode.value = "";
                    mobile.value = "";
                }
            });

            // ------------------ load cart items ------------------------
            let st_tbody = document.getElementById("st-tbody");
            let st_item_tr = document.getElementById("st-item-tr");
            let st_order_subtotal_tr = document.getElementById("st-order-subtotal-tr");
            let st_order_shipping_tr = document.getElementById("st-order-shipping-tr");
            let st_order_total_tr = document.getElementById("st-order-total-tr");

            st_tbody.innerHTML = "";

            let sub_total = 0;

            cartList.forEach(item => {

                let clone = st_item_tr.cloneNode(true);
                clone.querySelector("#st-item-title").innerHTML = item.product.title;
                clone.querySelector("#st-item-qty").innerHTML = item.qty;

                let item_sub_total = item.product.price * item.qty;
                sub_total += item_sub_total;

                clone.querySelector("#st-item-subtotal").innerHTML =
                        new Intl.NumberFormat("en-US", { minimumFractionDigits: 2 }).format(item_sub_total);

                st_tbody.appendChild(clone);
            });

            st_order_subtotal_tr.querySelector("#st-subtotal").innerHTML =
                    new Intl.NumberFormat("en-US", { minimumFractionDigits: 2 }).format(sub_total);
            st_tbody.appendChild(st_order_subtotal_tr);

            // shipping + total update
            citySelect.addEventListener("change", e => {

                let item_count = cartList.length;
                let shipping_amount = 0;

                if (citySelect.value === "1") {
                    shipping_amount = item_count * 1000; // Colombo
                } else {
                    shipping_amount = item_count * 2500;
                }

                st_order_shipping_tr.querySelector("#st-shipping-amount").innerHTML =
                        new Intl.NumberFormat("en-US", { minimumFractionDigits: 2 }).format(shipping_amount);
                st_tbody.appendChild(st_order_shipping_tr);

                let total = sub_total + shipping_amount;
                st_order_total_tr.querySelector("#st-total").innerHTML =
                        new Intl.NumberFormat("en-US", { minimumFractionDigits: 2 }).format(total);
                st_tbody.appendChild(st_order_total_tr);
            });

            citySelect.dispatchEvent(new Event("change"));

        } else {
            window.location = "sign-in.html";
        }
    }
}

// ---------------- checkout() function ---------------------
async function checkout() {

    //check address status
    let isCurrentAddress = document.getElementById("checkbox1").checked;

    //get address data
    let firstName = document.getElementById("first-name");
    let lastName = document.getElementById("last-name");
    let city = document.getElementById("city");
    let address1 = document.getElementById("address1");
    let address2 = document.getElementById("address2");
    let postalCode = document.getElementById("postal-code");
    let mobile = document.getElementById("mobile");

    //request data (json)
    const data = {
        isCurrentAddress: isCurrentAddress,
        firstName: firstName.value,
        lastName: lastName.value,
        cityId: city.value,
        address1: address1.value,
        address2: address2.value,
        postalCode: postalCode.value,
        mobile: mobile.value
    };

    const response = await fetch("Checkout", {
        method: "POST",
        body: JSON.stringify(data),
        headers: {
            "Content-Type": "application/json"
        }
    });

    if (response.ok) {
        const json = await response.json();
        console.log(json);

        if (json.success) {

            console.log(json.payhereJson);
            payhere.startPayment(json.payhereJson);
        } else {
            popup.error({
                message: json.message
            });
        }

    } else {
        popup.error({
            message: "Please try again later"
        });
    }

}