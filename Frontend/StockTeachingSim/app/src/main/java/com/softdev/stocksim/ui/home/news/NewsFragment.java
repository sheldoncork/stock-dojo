package com.softdev.stocksim.ui.home.news;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.softdev.stocksim.R;
import com.softdev.stocksim.api.VolleySingleton;
import com.softdev.stocksim.ui.BaseFragment;
import com.softdev.stocksim.utils.AppConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author Sheldon
 */
public class NewsFragment extends BaseFragment {
    private final String TAG = "NewsFragment";
    private final String ARG_STOCK = "stockSymbol";

    private String stockSymbol;

    private ArrayList<Article> articles = new ArrayList<>();
    private RecyclerView recyclerView;

    public NewsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            stockSymbol = getArguments().getString(ARG_STOCK);
        }
    }

    @Override
    protected View onCreateContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        showLoading();
        initViews(view);
        if(stockSymbol == null) {
            fetchNews(""); // by default gets general news
        }
        else {
            fetchNews("/company?symbol=" + stockSymbol);
        }
        return view;
    }

    private void initViews(View view){
        // add buttons for other categories?
        recyclerView = view.findViewById(R.id.news_recycler);
    }

    private void fetchNews(String appendUrl){
        String url = AppConfig.BASE_URL + "/news" + appendUrl;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET,
                url,
                null,
                this::handleNewsResponse,
                error ->{
                    Log.e(TAG, "Error fetching news data", error);
                    showError("Error fetching news. Try again soon.");
                });
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(request);
    }

    private void handleNewsResponse(JSONArray jsonArray) {
        Log.d(TAG, "News data received: " + jsonArray.toString());

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Article article = new Article();
                article.setCategory(jsonObject.getString("category"));
                article.setDatetime(jsonObject.getLong("datetime"));
                article.setHeadline(jsonObject.getString("headline"));
                article.setId(jsonObject.getInt("id"));
                article.setImage(jsonObject.getString("image"));
                article.setSource(jsonObject.getString("source"));
                article.setSummary(jsonObject.getString("summary"));
                article.setUrl(jsonObject.getString("url"));

                articles.add(article);
            } catch (JSONException e){
                Log.e(TAG, "Error parsing news JSON: ", e);
            }
        }
        setupRecycler();
    }

    private void setupRecycler(){
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        ArticleAdapter articleAdapter = new ArticleAdapter(getContext(), articles);
        recyclerView.setAdapter(articleAdapter);
        hideLoading();
    }
}