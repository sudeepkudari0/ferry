package com.sudeepkudari.ferry.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sudeepkudari.ferry.R;
import com.sudeepkudari.ferry.agent.AgentTaskService;
import com.sudeepkudari.ferry.model.UseCase;
import com.sudeepkudari.ferry.model.UseCaseParameter;
import com.sudeepkudari.ferry.net.PortalClient;

import java.util.HashMap;
import java.util.Map;

public class UseCaseBottomSheetFragment extends BottomSheetDialogFragment {

    private static UseCase currentUseCase;
    private final Map<String, View> inputViews = new HashMap<>();

    public static UseCaseBottomSheetFragment newInstance(UseCase useCase) {
        UseCaseBottomSheetFragment fragment = new UseCaseBottomSheetFragment();
        currentUseCase = useCase;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_use_case, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentUseCase == null) {
            dismiss();
            return;
        }

        ImageView icon = view.findViewById(R.id.sheetIconImageView);
        TextView title = view.findViewById(R.id.sheetTitleTextView);
        TextView subtitle = view.findViewById(R.id.sheetSubtitleTextView);
        LinearLayout parametersContainer = view.findViewById(R.id.parametersContainer);
        View runButton = view.findViewById(R.id.runUseCaseButton);

        icon.setImageResource(currentUseCase.getIconResId());
        title.setText(currentUseCase.getTitle());
        subtitle.setText(currentUseCase.getSubtitle());

        buildParameters(parametersContainer);

        runButton.setOnClickListener(v -> executeUseCase());
    }

    private void buildParameters(LinearLayout container) {
        for (UseCaseParameter param : currentUseCase.getParameters()) {
            View inputView = null;
            
            switch (param.getType()) {
                case TEXT:
                case MULTILINE_TEXT:
                    TextInputLayout textInputLayout = new TextInputLayout(requireContext());
                    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    textParams.setMargins(0, 0, 0, 32);
                    textInputLayout.setLayoutParams(textParams);
                    textInputLayout.setHint(param.getName());

                    TextInputEditText editText = new TextInputEditText(requireContext());
                    editText.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    if (param.getType() == UseCaseParameter.Type.MULTILINE_TEXT) {
                        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        editText.setMinLines(3);
                    }
                    textInputLayout.addView(editText);
                    
                    inputView = editText;
                    container.addView(textInputLayout);
                    break;
                    
                case RADIO:
                    TextView radioTitle = new TextView(requireContext());
                    radioTitle.setText(param.getName());
                    radioTitle.setPadding(0, 0, 0, 8);
                    container.addView(radioTitle);
                    
                    RadioGroup radioGroup = new RadioGroup(requireContext());
                    LinearLayout.LayoutParams radioParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    radioParams.setMargins(0, 0, 0, 32);
                    radioGroup.setLayoutParams(radioParams);
                    radioGroup.setOrientation(LinearLayout.HORIZONTAL);
                    
                    for (int i = 0; i < param.getOptions().size(); i++) {
                        String option = param.getOptions().get(i);
                        RadioButton rb = new RadioButton(requireContext());
                        rb.setText(option);
                        rb.setId(View.generateViewId());
                        radioGroup.addView(rb);
                        if (i == 0) rb.setChecked(true);
                    }
                    
                    inputView = radioGroup;
                    container.addView(radioGroup);
                    break;
                    
                case DROPDOWN:
                    TextView dropTitle = new TextView(requireContext());
                    dropTitle.setText(param.getName());
                    dropTitle.setPadding(0, 0, 0, 8);
                    container.addView(dropTitle);
                    
                    Spinner spinner = new Spinner(requireContext());
                    LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    spinnerParams.setMargins(0, 0, 0, 32);
                    spinner.setLayoutParams(spinnerParams);
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, param.getOptions());
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                    
                    inputView = spinner;
                    container.addView(spinner);
                    break;
            }
            
            if (inputView != null) {
                inputViews.put(param.getId(), inputView);
            }
        }
    }

    private void executeUseCase() {
        Map<String, String> inputs = new HashMap<>();
        for (UseCaseParameter param : currentUseCase.getParameters()) {
            View view = inputViews.get(param.getId());
            String value = "";
            
            if (view instanceof EditText) {
                value = ((EditText) view).getText().toString().trim();
                if (value.isEmpty()) {
                    ((EditText) view).setError("Required");
                    return;
                }
            } else if (view instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) view;
                int selectedId = rg.getCheckedRadioButtonId();
                if (selectedId != -1) {
                    RadioButton rb = rg.findViewById(selectedId);
                    value = rb.getText().toString();
                }
            } else if (view instanceof Spinner) {
                value = ((Spinner) view).getSelectedItem().toString();
            }
            
            inputs.put(param.getId(), value);
        }

        String finalPrompt = currentUseCase.buildPrompt(inputs);
        
        if (!PortalClient.isPortalInstalled(requireContext())) {
            Toast.makeText(requireContext(), "Cannot start: Accessibility Service is not enabled.", Toast.LENGTH_LONG).show();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(requireContext())) {
            Toast.makeText(requireContext(), "Cannot start: Please grant the 'Draw over other apps' permission.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent serviceIntent = new Intent(requireContext(), AgentTaskService.class);
        serviceIntent.putExtra(AgentTaskService.EXTRA_TASK, finalPrompt);
        requireContext().startForegroundService(serviceIntent);
        
        Toast.makeText(requireContext(), "Task started", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
