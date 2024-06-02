package com.example.pocketnews.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pocketnews.models.Article
import com.example.pocketnews.models.NewsResponse
import com.example.pocketnews.repository.NewsRepository
import com.example.pocketnews.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel(app: Application, val newsRepository: NewsRepository) : AndroidViewModel(app) {

    val headlines: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var headlinesPage = 1
    var headLinesResponse: NewsResponse? = null


    val searchNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    var searchNewsPage = 1
    var searchNewsResponse: NewsResponse? = null
    var newSearchQuery: String? = null
    var oldSearchQuery: String? = null

    init {
        getHandlines("us")
    }


    fun getHandlines(countryCode: String) = viewModelScope.launch {
        headlinesInternet(countryCode)

    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        searchNewsInternet(searchQuery)
    }


    private fun handleHeadlinesResponse(response: Response<NewsResponse>): Resource<NewsResponse> {

        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                headlinesPage++
                if (headLinesResponse == null) {
                    headLinesResponse = resultResponse
                } else {
                    val oldArticles = headLinesResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(headLinesResponse ?: resultResponse)
            }
        }

        return Resource.Error(response.message())
    }


    private fun handleSearchNewsResponse(response: Response<NewsResponse>): Resource<NewsResponse> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                if (searchNewsResponse == null || newSearchQuery != oldSearchQuery) {

                    searchNewsPage = 1
                    oldSearchQuery = newSearchQuery
                    searchNewsResponse = resultResponse

                } else {
                    searchNewsPage++
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = resultResponse.articles
                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsResponse ?: resultResponse)
            }
        }

        return Resource.Error(response.message())
    }


    fun addToFavourites(article: Article) = viewModelScope.launch {
        newsRepository.upsert(article)
    }

    fun getFavourites() = newsRepository.getFavouriteNews()


    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }


    fun internetConnection(context: Application): Boolean {
        (context.getSystemService(Application.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager).apply {

            return getNetworkCapabilities(activeNetwork)?.run {
                when {
                    hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } ?: false

        }

    }

    private suspend fun headlinesInternet(countryCode: String) {
        headlines.postValue(Resource.Loading())

        try {
            if (internetConnection(getApplication())) {
                val response = newsRepository.getHeadlines(countryCode, headlinesPage)
                headlines.postValue(handleHeadlinesResponse(response))
            } else {
                headlines.postValue(Resource.Error("No Internet Connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> headlines.postValue(Resource.Error("Network Failure"))
                else -> headlines.postValue(Resource.Error("Conversion Error"))
            }

        }


    }

    private suspend fun searchNewsInternet(searchQuery: String) {

        newSearchQuery = searchQuery
        searchNews.postValue(Resource.Loading())

        try {
            if (internetConnection(getApplication())) {
                val response = newsRepository.searchNews(searchQuery, searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                searchNews.postValue(Resource.Error("No Internet Connection"))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNews.postValue(Resource.Error("Network Failure"))
                else -> searchNews.postValue(Resource.Error("Conversion Error"))
            }

        }


    }

}