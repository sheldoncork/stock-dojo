package com.softdev.stocksim.ui.home.news;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.softdev.stocksim.R;

import java.util.List;

/**
 * @author Sheldon
 */
public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {
    private List<Article> articles;
    private Context context;

    public ArticleAdapter(Context context, List<Article> articles) {
        this.context = context;
        this.articles = articles;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);

        holder.headline.setText(article.getHeadline());
        holder.source.setText(article.getSource());
        holder.date.setText(article.getDatetime());
        holder.summary.setText(article.getSummary());

        // Load the image using Glide
        Glide.with(context)
                .load(article.getImage())
                .into(holder.articleImage);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        TextView headline, source, date, summary;
        ImageView articleImage;

        public ArticleViewHolder(View itemView) {
            super(itemView);
            headline = itemView.findViewById(R.id.headline);
            source = itemView.findViewById(R.id.source);
            date = itemView.findViewById(R.id.date_tv);
            summary = itemView.findViewById(R.id.summary);
            articleImage = itemView.findViewById(R.id.article_image);
        }
    }
}
