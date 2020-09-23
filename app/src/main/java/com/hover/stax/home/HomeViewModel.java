package com.hover.stax.home;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.hover.sdk.transactions.TransactionContract;
import com.hover.stax.actions.Action;
import com.hover.stax.channels.Channel;
import com.hover.stax.database.DatabaseRepo;
import com.hover.stax.transactions.StaxTransaction;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {
	private final String TAG = "HomeViewModel";

	private LiveData<List<Channel>> selectedChannels;
	private LiveData<List<Action>> balanceActions;

	private LiveData<List<StaxTransaction>> transactions;

	private DatabaseRepo repo;

	public HomeViewModel(Application application) {
		super(application);
		repo = new DatabaseRepo(application);
		if (selectedChannels == null) { selectedChannels = new MutableLiveData<>(); }
		selectedChannels = repo.getSelected();
		balanceActions = Transformations.switchMap(selectedChannels, this::loadBalanceActions);

		transactions = new MutableLiveData<>();
		transactions = repo.getCompleteTransferTransactions();
	}

	public LiveData<List<StaxTransaction>> getStaxTransactions() { return transactions; }

	public LiveData<List<Action>> loadBalanceActions(List<Channel> channelList) {
		int[] ids = new int[channelList.size()];
		for (int c = 0; c < channelList.size(); c++) {
			ids[c] = channelList.get(c).id;
		}
		return repo.getLiveActions(ids, Action.BALANCE);
	}

	public LiveData<List<Channel>> getSelectedChannels() {
		if (selectedChannels == null) { selectedChannels = new MutableLiveData<>(); }
		return selectedChannels;
	}

	public Channel getChannel(int id) {
		List<Channel> allChannels = selectedChannels.getValue() != null ? selectedChannels.getValue() : new ArrayList<>();
		for (Channel channel : allChannels) {
			if (channel.id == id) {
				return channel;
			}
		}
		return null;
	}

	public LiveData<List<Action>> getBalanceActions() {
		if (balanceActions == null) { balanceActions = new MutableLiveData<>(); }
		return balanceActions;
	}

	public List<Action> getBalanceActions(int channel_id) {
		return repo.getActions(channel_id, Action.BALANCE);
	}

	public void saveTransaction(Intent data, Context c) {
		new Thread(() -> {
			StaxTransaction t = new StaxTransaction(data, repo.getAction(data.getStringExtra(TransactionContract.COLUMN_ACTION_ID)), c);
			if (t.uuid != null) { repo.insert(t); }
		}).start();
	}

}