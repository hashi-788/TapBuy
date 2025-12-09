package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dto.User_DTO;
import entity.Address;
import entity.Cart;
import entity.City;
import entity.Order_Status;
import entity.Order_item;
import entity.Product;
import entity.User;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.HibernateUtil;
import model.Payhere;
import model.Validations;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "Checkout", urlPatterns = {"/Checkout"})
public class Checkout extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();

        JsonObject requestJsonObject = gson.fromJson(request.getReader(), JsonObject.class);

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("success", false);

        HttpSession httpSession = request.getSession();

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        boolean isCurrentAddress = requestJsonObject.get("isCurrentAddress").getAsBoolean();
        String firstName = requestJsonObject.get("firstName").getAsString();
        String lastName = requestJsonObject.get("lastName").getAsString();
        String cityId = requestJsonObject.get("cityId").getAsString();
        String address1 = requestJsonObject.get("address1").getAsString();
        String address2 = requestJsonObject.get("address2").getAsString();
        String postalCode = requestJsonObject.get("postalCode").getAsString();
        String mobile = requestJsonObject.get("mobile").getAsString();

        if (httpSession.getAttribute("user") != null) {

            User_DTO user_DTO = (User_DTO) httpSession.getAttribute("user");
            Criteria criteria1 = session.createCriteria(User.class);
            criteria1.add(Restrictions.eq("email", user_DTO.getEmail()));
            User user = (User) criteria1.uniqueResult();

            if (isCurrentAddress) {
                Criteria criteria2 = session.createCriteria(Address.class);
                criteria2.add(Restrictions.eq("user", user));
                criteria2.addOrder(Order.desc("id"));
                criteria2.setMaxResults(1);

                if (criteria2.list().isEmpty()) {
                    responseJsonObject.addProperty("message", "Current address not found. Please create a new address");
                } else {
                    Address address = (Address) criteria2.list().get(0);

                    saveOrders(session, transaction, user, address, responseJsonObject);
                }

            } else {

                if (firstName.isEmpty()) {
                    responseJsonObject.addProperty("message", "Please fill first name");

                } else if (lastName.isEmpty()) {
                    responseJsonObject.addProperty("message", "Please fill last name");

                } else if (!Validations.isInteger(cityId)) {
                    responseJsonObject.addProperty("message", "Invalid City");

                } else {

                    Criteria criteria3 = session.createCriteria(City.class);
                    criteria3.add(Restrictions.eq("id", Integer.parseInt(cityId)));

                    if (criteria3.list().isEmpty()) {
                        responseJsonObject.addProperty("message", "Invalid city selected");

                    } else {
                        City city = (City) criteria3.list().get(0);

                        if (address1.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill address line 1");

                        } else if (address2.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill address line 2");

                        } else if (postalCode.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill postal code");

                        } else if (postalCode.length() != 5) {
                            responseJsonObject.addProperty("message", "Invalid postal code");

                        } else if (!Validations.isInteger(postalCode)) {
                            responseJsonObject.addProperty("message", "Invalid postal code");

                        } else if (mobile.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill mobile number");

                        } else if (!Validations.isMobileNumberValid(mobile)) {
                            responseJsonObject.addProperty("message", "Invalid mobile number");

                        } else {

                            Address address = new Address();
                            address.setCity(city);
                            address.setFirst_name(firstName);
                            address.setLast_name(lastName);
                            address.setLine1(address1);
                            address.setLine2(address2);
                            address.setMobile(mobile);
                            address.setPostal_code(postalCode);
                            address.setUser(user);

                            session.save(address);
                            saveOrders(session, transaction, user, address, responseJsonObject);
                        }
                    }

                }
            }

        } else {
            responseJsonObject.addProperty("message", "User not signed in");
        }

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseJsonObject));
    }

    private void saveOrders(Session session, Transaction transaction, User user, Address address, JsonObject responseJsonObject) {
        try {
            entity.Orders order = new entity.Orders();
            order.setAddress(address);
            order.setDate_time(new Date());
            order.setUser(user);
            int order_id = (int) session.save(order);

            Criteria criteria4 = session.createCriteria(Cart.class);
            criteria4.add(Restrictions.eq("user", user));
            List<Cart> cartList = criteria4.list();

            Order_Status order_Status = (Order_Status) session.get(Order_Status.class, 5);

            double amount = 0;
            String items = "";
            for (Cart cartItem : cartList) {

                amount += cartItem.getProduct().getPrice() * cartItem.getQty();
                if (address.getCity().getId() == 1) {
                    amount += 1000;
                } else {
                    amount += 2500;
                }

                items += cartItem.getProduct().getTitle() + " x" + cartItem.getQty() + " ";

                Product product = cartItem.getProduct();

                Order_item order_item = new Order_item();
                order_item.setOrder(order);
                order_item.setOrder_status(order_Status);
                order_item.setProduct(product);
                order_item.setQty(cartItem.getQty());
                session.save(order_item);

                product.setQty(product.getQty() - cartItem.getQty());
                session.update(product);

                session.delete(cartItem);

            }

            transaction.commit();

            String merchant_id = "1221860";
            String formatted_amount = new DecimalFormat("0.00").format(amount);
            String currency = "LKR";
            String merchantSecret = "MjI5NTMxMDIwMDIyOTgwMzM3NTY3NjY3NDA4NzcyMzYwODM0MTUy"; //**
            String merchantSecretMdHash = Payhere.generateMD5(merchantSecret);

            JsonObject payhere = new JsonObject();
            payhere.addProperty("merchant_id", merchant_id);

            payhere.addProperty("return_url", "");
            payhere.addProperty("cancel_url", "");
            payhere.addProperty("notify_url", " https://c1f9273ec985.ngrok-free.app/SmartTrade/VerifyPayment"); //***

            payhere.addProperty("first_name", user.getFirst_name());
            payhere.addProperty("last_name", user.getLast_name());
            payhere.addProperty("email", user.getEmail());

            payhere.addProperty("phone", "");
            payhere.addProperty("address", "");
            payhere.addProperty("city", "");
            payhere.addProperty("country", "");

            payhere.addProperty("order_id", String.valueOf(order_id));
            payhere.addProperty("items", items);
            payhere.addProperty("currency", "LKR");
            payhere.addProperty("amount", formatted_amount);
            payhere.addProperty("sandbox", true);

            String md5Hash = Payhere.generateMD5(merchant_id + order_id + formatted_amount + currency + merchantSecretMdHash);
            payhere.addProperty("hash", md5Hash);

            responseJsonObject.addProperty("success", true);
            responseJsonObject.addProperty("message", "Checkout completed");

            Gson gson = new Gson();
            responseJsonObject.add("payhereJson", gson.toJsonTree(payhere));

        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
            responseJsonObject.addProperty("message", e.getMessage());
        }
    }
}
