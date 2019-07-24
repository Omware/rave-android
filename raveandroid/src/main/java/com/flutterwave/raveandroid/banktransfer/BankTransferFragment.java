package com.flutterwave.raveandroid.banktransfer;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.flutterwave.raveandroid.Payload;
import com.flutterwave.raveandroid.PayloadBuilder;
import com.flutterwave.raveandroid.R;
import com.flutterwave.raveandroid.RaveConstants;
import com.flutterwave.raveandroid.RavePayActivity;
import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.Utils;
import com.flutterwave.raveandroid.ViewObject;
import com.flutterwave.raveandroid.responses.ChargeResponse;

import java.util.HashMap;

import static android.view.View.GONE;

/**
 * A simple {@link Fragment} subclass.
 */
public class BankTransferFragment extends Fragment implements BankTransferContract.View, View.OnClickListener {

    View v;
    TextInputEditText amountEt;
    TextInputLayout amountTil;
    TextView amountTv;
    TextView accountNumberTv;
    TextView bankNameTv;
    TextView beneficiaryNameTv;
    TextView transferInstructionTv;
    TextView transferStatusTv;
    Button verifyPaymentButton;
    Button payButton;
    ConstraintLayout initiateChargeLayout;
    ConstraintLayout transferDetailsLayout;
    RavePayInitializer ravePayInitializer;
    private ProgressDialog progressDialog;
    private ProgressDialog pollingProgressDialog;
    BankTransferPresenter presenter;

    public BankTransferFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        presenter = new BankTransferPresenter(getActivity(), this);

        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_bank_transfer, container, false);


        initializeViews();

        setListeners();

        ravePayInitializer = ((RavePayActivity) getActivity()).getRavePayInitializer();

        presenter.init(ravePayInitializer);

        return v;
    }

    private void setListeners() {
        payButton.setOnClickListener(this);
        verifyPaymentButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == payButton.getId()) {
            clearErrors();
            Utils.hide_keyboard(getActivity());
            collectData();
        }

        if (viewId == verifyPaymentButton.getId()) {
            verifyPayment();
        }
    }

    private void collectData() {
        HashMap<String, ViewObject> dataHashMap = new HashMap<>();

        dataHashMap.put(RaveConstants.fieldAmount, new ViewObject(amountTil.getId(), amountEt.getText().toString(), TextInputLayout.class));
        presenter.onDataCollected(dataHashMap);
    }

    private void initializeViews() {
        amountEt = (TextInputEditText) v.findViewById(R.id.rave_amountTV);
        amountTil = (TextInputLayout) v.findViewById(R.id.rave_amountTil);
        initiateChargeLayout = (ConstraintLayout) v.findViewById(R.id.rave_initiate_payment_layout);
        transferDetailsLayout = (ConstraintLayout) v.findViewById(R.id.rave_transfer_details_layout);
        transferInstructionTv = (TextView) v.findViewById(R.id.rave_bank_transfer_instruction);
        transferStatusTv = (TextView) v.findViewById(R.id.rave_transfer_status_tv);
        amountTv = (TextView) v.findViewById(R.id.rave_amount_tv);
        beneficiaryNameTv = (TextView) v.findViewById(R.id.rave_beneficiary_name_tv);
        bankNameTv = (TextView) v.findViewById(R.id.rave_bank_name_tv);
        accountNumberTv = (TextView) v.findViewById(R.id.rave_account_number_tv);
        payButton = (Button) v.findViewById(R.id.rave_payButton);
        verifyPaymentButton = (Button) v.findViewById(R.id.rave_verify_payment_button);
    }

    @Override
    public void onAmountValidationSuccessful(String amountToPay) {
        amountTil.setVisibility(GONE);
        amountEt.setText(amountToPay);
    }


    @Override
    public void showFieldError(int viewID, String message, Class<?> viewType) {

        if (viewType == TextInputLayout.class) {
            TextInputLayout view = v.findViewById(viewID);
            view.setError(message);
        } else if (viewType == EditText.class) {
            EditText view = v.findViewById(viewID);
            view.setError(message);
        }

    }


    private void verifyPayment() {
        showPollingIndicator(true);
        presenter.startPaymentVerification();

    }


    @Override
    public void showPollingIndicator(boolean active) {
        if (getActivity() != null) {
            if (getActivity().isFinishing())
                return;
        }

        if (pollingProgressDialog == null) {
            pollingProgressDialog = new ProgressDialog(getActivity());
            pollingProgressDialog.setMessage("Checking transaction status. \nPlease wait");
        }

        if (active && !pollingProgressDialog.isShowing()) {
            pollingProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pollingProgressDialog.dismiss();
                }
            });
            pollingProgressDialog.show();
        } else if (active && pollingProgressDialog.isShowing()) {
            //pass
        } else {
            pollingProgressDialog.dismiss();
        }

    }

    private void clearErrors() {
        amountTil.setError(null);

        amountTil.setErrorEnabled(false);

    }

    @Override
    public void onTransferDetailsReceived(ChargeResponse response) {
        showTransferDetails(response);
    }

    @Override
    public void onPollingTimeout(String flwRef, String txRef, final String responseAsJSONString) {
        transferStatusTv.setText(getString(R.string.pay_with_bank_timeout_notification));
        transferStatusTv.setVisibility(View.VISIBLE);

        verifyPaymentButton.setText(getString(R.string.back_to_app));
        verifyPaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("response", responseAsJSONString);
                if (getActivity() != null) {
                    getActivity().setResult(RavePayActivity.RESULT_ERROR, intent);
                    getActivity().finish();
                }
            }
        });
    }

    private void showTransferDetails(ChargeResponse response) {
        String beneficiaryName = response.getData().getNote().substring(
                response.getData().getNote().indexOf("to ") + 3
        );

        amountTv.setText(response.getData().getAmount());
        beneficiaryNameTv.setText(beneficiaryName);
        bankNameTv.setText(response.getData().getBankname());
        accountNumberTv.setText(response.getData().getAccountnumber());
        transferInstructionTv.setText(
                transferInstructionTv.getText() + " " + beneficiaryName
        );

        initiateChargeLayout.setVisibility(GONE);
        transferDetailsLayout.setVisibility(View.VISIBLE);


    }


    @Override
    public void showProgressIndicator(boolean active) {

        if (getActivity().isFinishing()) {
            return;
        }

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setMessage("Please wait...");
        }

        if (active && !progressDialog.isShowing()) {
            progressDialog.show();
        } else if (active && progressDialog.isShowing()) {
            //pass
        } else {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onPaymentError(String message) {
//        dismissDialog();
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaymentSuccessful(String status, String flwRef, final String responseAsString) {
        Intent intent = new Intent();
        intent.putExtra("response", responseAsString);

        if (getActivity() != null) {
            getActivity().setResult(RavePayActivity.RESULT_SUCCESS, intent);
            getActivity().finish();
        }
    }

    @Override
    public void onPaymentFailed(String message, final String responseAsJSONString) {
        Intent intent = new Intent();
        intent.putExtra("response", responseAsJSONString);
        if (getActivity() != null) {
            getActivity().setResult(RavePayActivity.RESULT_ERROR, intent);
            getActivity().finish();
        }

    }

    @Override
    public void displayFee(String charge_amount, final Payload payload) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("You will be charged a total of " + charge_amount + ravePayInitializer.getCurrency() + ". Do you want to continue?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                presenter.payWithBankTransfer(payload, ravePayInitializer.getEncryptionKey());


            }
        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    @Override
    public void showFetchFeeFailed(String s) {
        showToast(s);
    }

    @Override
    public void onValidationSuccessful(HashMap<String, ViewObject> dataHashMap) {

        ravePayInitializer.setAmount(Double.parseDouble(dataHashMap.get(RaveConstants.fieldAmount).getData()));
        presenter.processTransaction(dataHashMap, ravePayInitializer);

    }

    @Override
    public void onAmountValidationFailed() {
        amountTil.setVisibility(View.VISIBLE);
    }

}
