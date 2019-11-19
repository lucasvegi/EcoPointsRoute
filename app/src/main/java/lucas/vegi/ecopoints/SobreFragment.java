package lucas.vegi.ecopoints;

import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class SobreFragment extends Fragment {

	public SobreFragment(){}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
 
        View rootView = inflater.inflate(R.layout.fragment_sobre, container, false);
         
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView appVersion = (TextView) getActivity().findViewById(R.id.versaoSobre);
        PackageInfo pInfo;

        //OBTEM A VERSÃO DO APLICATIVO
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            appVersion.setText("Versão " + version);

            ImageView img = (ImageView) getActivity().findViewById(R.id.fundo_texto_sobre);

            //ANIMAÇÃO ALPHA PARA FUNDO DO TEXTO NA API LEVEL 8
            AlphaAnimation alpha = new AlphaAnimation(0.8F, 0.8F);
            alpha.setDuration(0); // Make animation instant
            alpha.setFillAfter(true); // Tell it to persist after the animation ends
            img.startAnimation(alpha);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


    }
}
