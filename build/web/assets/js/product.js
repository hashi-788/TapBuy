var st_product = document.getElementById("st-product-template");
var currentPage = 0;

window.onload = function() {
    loadData();

    document.getElementById("apply-btn").addEventListener("click", () => searchProducts(0));
    document.getElementById("reset-btn").addEventListener("click", () => loadData());
};

async function loadData() {
    const response = await fetch("LoadData");
    const popup = Notification();

    if (response.ok) {
        const json = await response.json();
        //console.log(json);

        loadOption("category", json.categoryList, "name");
        loadOption("color", json.colorList, "name");
        loadOption("storage", json.storageList, "value");

        updateProductView(json);
    } else {
        popup.error({ message: "Try again later" });
    }
}

function loadOption(prefix, dataList, property) {
    let options = document.getElementById(prefix + "-options");
    let li = document.getElementById(prefix + "-li");
    options.innerHTML = "";

    dataList.forEach(data => {
        let li_clone = li.cloneNode(true);

        if (prefix === "color") {
            li_clone.style.borderColor = data[property];
            li_clone.querySelector("#" + prefix + "-button").style.backgroundColor = data[property];
        } else {
            li_clone.querySelector("#" + prefix + "-button").innerHTML = data[property];
        }

        options.appendChild(li_clone);
    });

    const all_li = document.querySelectorAll("#" + prefix + "-options li");
    all_li.forEach(x => {
        x.addEventListener('click', function () {
            all_li.forEach(y => y.classList.remove('chosen'));
            this.classList.add('chosen');
        });
    });
}

async function searchProducts(firstResult) {
    const popup = Notification();

    let category_name = document.getElementById("category-options").querySelector(".chosen")?.querySelector("button").innerHTML;
    let color = document.getElementById("color-options").querySelector(".chosen")?.querySelector("button").style.backgroundColor;
    let storage = document.getElementById("storage-options").querySelector(".chosen")?.querySelector("button").innerHTML;
    let price_range_start = document.getElementById("coffee-min-range").value;
    let price_range_end = document.getElementById("coffee-max-range").value;
    let sort_text = document.getElementById("st-sort").value;

    const data = { firstResult, category_name, color, storage, price_range_start, price_range_end, sort_text };

    const response = await fetch("SearchProducts", {
        method: "POST",
        body: JSON.stringify(data),
        headers: { "Content-Type": "application/json" }
    });

    if (response.ok) {
        const json = await response.json();
        //console.log(json);
        updateProductView(json);
    } else {
        popup.error({ message: "Try again later" });
    }
}

function updateProductView(json) {
    const st_product_container = document.getElementById("st-product-container");
    const paginationList = document.getElementById("st-pagination-list");

    st_product_container.innerHTML = "";
    paginationList.innerHTML = "";

    if (!json.productList || json.productList.length === 0) {
        st_product_container.innerHTML = "<p>No products found</p>";
        return;
    }

    json.productList.forEach(product => {
        const st_product_clone = st_product.cloneNode(true);
        const aTag = st_product_clone.querySelector(".st-product-a");
        const imgTag = st_product_clone.querySelector(".st-product-img");
        const titleTag = st_product_clone.querySelector(".st-product-title");
        const priceTag = st_product_clone.querySelector(".st-product-price");

        aTag.href = "single-product-view.html?id=" + product.id;
        imgTag.src = "product-images/" + product.id + "/image1.png";
        titleTag.textContent = product.title;
        priceTag.textContent = "Rs. " + new Intl.NumberFormat("en-US").format(product.price);

        st_product_clone.classList.remove("st-product-template");
        st_product_clone.style.display = "block";

        st_product_container.appendChild(st_product_clone);
    });

    const product_count = json.allProductCount;
    const product_per_page = 12;
    const pages = Math.ceil(product_count / product_per_page);

    const prevItem = document.createElement("li");
    prevItem.className = "page-item" + (currentPage === 0 ? " disabled" : "");
    const prevLink = document.createElement("a");
    prevLink.className = "page-link";
    prevLink.href = "#";
    prevLink.textContent = "Previous";
    prevLink.addEventListener("click", e => {
        e.preventDefault();
        if (currentPage > 0) {
            currentPage--;
            searchProducts(currentPage * product_per_page);
        }
    });
    prevItem.appendChild(prevLink);
    paginationList.appendChild(prevItem);

    for (let i = 0; i < pages; i++) {
        const pageItem = document.createElement("li");
        pageItem.className = "page-item" + (i === currentPage ? " active" : "");
        const pageLink = document.createElement("a");
        pageLink.className = "page-link";
        pageLink.href = "#";
        pageLink.textContent = i + 1;
        pageLink.addEventListener("click", e => {
            e.preventDefault();
            currentPage = i;
            searchProducts(currentPage * product_per_page);
        });
        pageItem.appendChild(pageLink);
        paginationList.appendChild(pageItem);
    }

    const nextItem = document.createElement("li");
    nextItem.className = "page-item" + (currentPage === pages - 1 ? " disabled" : "");
    const nextLink = document.createElement("a");
    nextLink.className = "page-link";
    nextLink.href = "#";
    nextLink.textContent = "Next";
    nextLink.addEventListener("click", e => {
        e.preventDefault();
        if (currentPage < pages - 1) {
            currentPage++;
            searchProducts(currentPage * product_per_page);
        }
    });
    nextItem.appendChild(nextLink);
    paginationList.appendChild(nextItem);
}
