package net.karmacoder.foaas;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.squareup.moshi.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import okhttp3.*;

public class MainActivity extends Activity 
{
	private final static String TAG = "MAIN";
	private final static String BASE_URL = "https://foaas.com";

	public static class Field
	{
		String name;
		String field;
	}

	public static class Operation
	{
		String name;
		String url;
		List<Field> fields;
	}

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		final OkHttpClient client = new OkHttpClient();
		final Request request = new Request.Builder()
			.url(BASE_URL + "/operations").build();

		client.newCall(request).enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e)
				{
					Log.e(TAG, "No operations.", e);
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException
				{
					final String ops = response.body().string();
					Log.d(TAG, ops);

					final Moshi moshi = new Moshi.Builder().build();
					final Type type = Types.newParameterizedType(List.class, Operation.class);

					final JsonAdapter<List<Operation>> jsonAdapter = moshi.adapter(type);
					final List<Operation> operations = jsonAdapter.fromJson(ops);

					runOnUiThread(new Runnable() {
							@Override
							public void run()
							{
								selectOperation(operations);
							}
						}
					);
				}
			}
		);
	}

	private void selectOperation(final List<Operation> operations)
	{
		final CharSequence[] operationNames = new CharSequence[operations.size()];
		for (final Operation operation : operations)
		{
			final int i = operations.indexOf(operation);
			operationNames[i] = operation.name;
		}

		new AlertDialog.Builder(MainActivity.this)
			.setTitle("Which operation do you want?")
			.setItems(operationNames, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int index)
				{
					final Operation operation = operations.get(index);
					showOperationConfirmation(operation);
				}
			}
		).show();
	}

	private void showOperationConfirmation(final Operation operation)
	{
		final LayoutInflater inflator = LayoutInflater.from(this);
		final View view = inflator.inflate(R.layout.operation, null, false);

		final TextView preview = view.findViewById(R.id.preview);
		final String previewText = operation.name + "<br><br><tt>" + operation.url + "</tt>";
		preview.setText(Html.fromHtml(previewText, 0));

		final LinearLayout fieldsLayout = view.findViewById(R.id.fields);
		final Map<String, EditText> fieldViewMap = new HashMap<>();

		for (final Field field : operation.fields)
		{
			final View item = inflator.inflate(R.layout.item, null, false);
			final EditText text = item.findViewById(R.id.text);
			text.setHint(field.name);

			fieldViewMap.put(field.field, text);

			fieldsLayout.addView(item);
		}

		new AlertDialog.Builder(this)
			.setTitle("Configure operation")
			.setView(view)
			.setPositiveButton(android.R.string.copy, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int index)
				{
					final Map<String,String> fieldMap = new HashMap<>();
					for(final String key : fieldViewMap.keySet()){
						fieldMap.put(key, fieldViewMap.get(key).getText().toString());
					}
					operatonConfigured(operation, fieldMap);
				}
			})
			.show();
	}

	private String buildUrl(Operation operation, Map<String, String> fieldMap)
	{
		String url = BASE_URL + operation.url;

		for (final Field field : operation.fields)
		{
			url = url.replace(":" + field.field, fieldMap.get(field.field));
		}

		return url;
	}

	private void operatonConfigured(final Operation operation, Map<String, String> fieldMap)
	{
		final String url = buildUrl(operation, fieldMap);

		final OkHttpClient client = new OkHttpClient();
		final Request request = new Request.Builder()
			.addHeader("accept", "text/plain")
			.url(url)
			.build();

		client.newCall(request).enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e)
				{
					Log.e(TAG, "No operation '" + operation.name + "' found.", e);
				}

				@Override
				public void onResponse(Call call, final Response response)
				{
					runOnUiThread(new Runnable(){
							@Override
							public void run()
							{
								try
								{
									final String text = response.body().string();
									final android.content.ClipboardManager manager = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
									manager.setText(text);

									final String toastText = "<br><b>"+text+"</b> copied to clipboard.<br><br><small>"+url+"</small>";
									Toast.makeText(MainActivity.this, Html.fromHtml(toastText,0), Toast.LENGTH_LONG).show();									
								}
								catch (Throwable th)
								{
									Log.e(TAG, "Could not load preview.", th);
								}
								finally
								{
									finish();
								}
							}
						}
					);
				}
			}
		);
	}
}
