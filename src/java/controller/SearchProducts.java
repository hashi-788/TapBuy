package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.Category;
import entity.Color;
import entity.Model;
import entity.Product;
import entity.Storage;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SearchProducts", urlPatterns = {"/SearchProducts"})
public class SearchProducts extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("success", false);

        JsonObject requestJsonObject = gson.fromJson(request.getReader(), JsonObject.class);

        Session session = HibernateUtil.getSessionFactory().openSession();

        Criteria criteria1 = session.createCriteria(Product.class);

        if (requestJsonObject.has("category_name")) {
            String categoryName = requestJsonObject.get("category_name").getAsString();

            Criteria criteria2 = session.createCriteria(Category.class);
            criteria2.add(Restrictions.eq("name", categoryName));
            List<Category> categoryList = criteria2.list();

            Criteria criteria3 = session.createCriteria(Model.class);
            criteria3.add(Restrictions.in("category", categoryList));
            List<Model> modelList = criteria3.list();

            criteria1.add(Restrictions.in("model", modelList));
        }

        if (requestJsonObject.has("color")) {

            String color = requestJsonObject.get("color").getAsString();

            Criteria criteria5 = session.createCriteria(Color.class);
            criteria5.add(Restrictions.eq("name", color));
            Color productColor = (Color) criteria5.uniqueResult();

            criteria1.add(Restrictions.eq("color", productColor));
        }

        if (requestJsonObject.has("storage")) {

            String storage = requestJsonObject.get("storage").getAsString();

            Criteria criteria6 = session.createCriteria(Storage.class);
            criteria6.add(Restrictions.eq("value", storage));
            Storage storageValue = (Storage) criteria6.uniqueResult();

            criteria1.add(Restrictions.eq("storage", storageValue));
        }

        double startPrice = requestJsonObject.get("price_range_start").getAsDouble();
        double endPrice = requestJsonObject.get("price_range_end").getAsDouble();

        criteria1.add(Restrictions.ge("price", startPrice));
        criteria1.add(Restrictions.le("price", endPrice));

        String sortText = requestJsonObject.get("sort_text").getAsString();

        if (sortText.equals("Sort by Latest")) {
            criteria1.addOrder(Order.desc("id"));

        } else if (sortText.equals("Sort by Oldest")) {
            criteria1.addOrder(Order.asc("id"));

        } else if (sortText.equals("Sort by Name")) {
            criteria1.addOrder(Order.asc("title"));

        } else if (sortText.equals("Sort by price")) {
            criteria1.addOrder(Order.asc("price"));

        }

        responseJsonObject.addProperty("allProductCount", criteria1.list().size());

        int firstResult = requestJsonObject.get("firstResult").getAsInt();
        criteria1.setFirstResult(firstResult);
        criteria1.setMaxResults(12);

        List<Product> productList = criteria1.list();
        System.out.println(productList.size());

        responseJsonObject.addProperty("success", true);
        responseJsonObject.add("productList", gson.toJsonTree(productList));
        
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseJsonObject));

    }

}
