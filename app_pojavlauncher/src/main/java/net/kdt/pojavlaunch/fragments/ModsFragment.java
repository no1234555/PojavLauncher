package net.kdt.pojavlaunch.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.modmanager.ModData;
import net.kdt.pojavlaunch.modmanager.ModManager;
import net.kdt.pojavlaunch.modmanager.State;
import net.kdt.pojavlaunch.modmanager.api.Curseforge;
import net.kdt.pojavlaunch.modmanager.api.Github;
import net.kdt.pojavlaunch.modmanager.api.Modrinth;
import us.feras.mdv.MarkdownView;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ModsFragment extends Fragment {

    private static String filter = "Modrinth";

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_mods, container, false);

        ModAdapter modAdapter = new ModAdapter(this);
        RecyclerView modRecycler = view.findViewById(R.id.mods_recycler);
        modRecycler.setLayoutManager(new LinearLayoutManager(modRecycler.getContext()));
        modRecycler.setAdapter(modAdapter);

        String[] filters = new String[] {"Modrinth", "CurseForge", "Installed", "Core"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        Spinner filterSpinner = view.findViewById(R.id.filter_spinner);
        filterSpinner.setAdapter(filterAdapter);

        SearchView modSearch = view.findViewById(R.id.mods_search);
        modSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                loadDataIntoList(modAdapter, s, 0, true);
                return true;
            }
        });

        modRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if ((filter.equals("Modrinth") || filter.equals("CurseForge")) && !recyclerView.canScrollVertically(1) && modAdapter.mods.size() > 0) {
                    loadDataIntoList(modAdapter, "", modAdapter.getOffset(), false);
                }
            }
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                filter = filters[i];
                loadDataIntoList(modAdapter, "", 0, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        return view;
    }

    private void loadDataIntoList(ModAdapter modAdapter, String query, int offset, boolean refresh) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) return;
        if (refresh) modAdapter.reset();
        State.Instance selectedInstance = ModManager.getInstance("Default");

        if (filter.equals("Modrinth")) Modrinth.addProjectsToRecycler(modAdapter, selectedInstance.getGameVersion(), offset, query);
        else if (filter.equals("CurseForge")) Curseforge.addProjectsToRecycler(modAdapter, selectedInstance.getGameVersion(), offset, query);
        else if (filter.equals("Installed")) {
            ArrayList<ModData> mods = ModManager.listInstalledMods(selectedInstance.getName());
            if (mods.size() == 0) return;
            ArrayList<ModData> filtered = (ArrayList<ModData>) mods.stream().filter(mod -> mod.title.substring(0, query.length()).equalsIgnoreCase(query)).collect(Collectors.toList());
            if (filtered.size() > 0) {
                modAdapter.addMods(filtered);
                modAdapter.loadProjectPage(filtered.get(0), null);
            }
        }
        else if (filter.equals("Core")) {
            ArrayList<ModData> mods = ModManager.listCoreMods(selectedInstance.getGameVersion());
            if (mods.size() == 0) return;
            ArrayList<ModData> filtered = (ArrayList<ModData>) mods.stream().filter(mod -> mod.title.substring(0, query.length()).equalsIgnoreCase(query)).collect(Collectors.toList());
            if (filtered.size() > 0) {
                modAdapter.addMods(filtered);
                modAdapter.loadProjectPage(filtered.get(0), null);
            }
        }
    }

    public static class ModViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ModAdapter adapter;
        private final ImageView icon;
        private final TextView title;
        private final TextView compat;
        private final Switch enableSwitch;
        private ModData modData;

        public ModViewHolder(View view, ModAdapter adapter) {
            super(view);
            this.adapter = adapter;
            view.setOnClickListener(this);
            icon = view.findViewById(R.id.mod_icon);
            title = view.findViewById(R.id.mod_title);
            compat = view.findViewById(R.id.mod_details);
            enableSwitch = view.findViewById(R.id.mod_switch);

            /*enableSwitch.setOnClickListener(view1 -> installMod(filter));
            enableSwitch.setOnDragListener((view1, dragEvent) -> {
                installMod(filter);
                return true;
            });*/
        }

        public void installMod() {
            if (filter.equals("Core") || ModManager.isDownloading(modData.slug)) {
                enableSwitch.setChecked(true);
                return;
            }

            State.Instance selectedInstance = ModManager.getInstance("Default");
            ModData mod = selectedInstance.getMod(modData.slug);
            if (mod != null) ModManager.setModActive(selectedInstance.getName(), modData.slug, enableSwitch.isChecked());
            else ModManager.addMod(selectedInstance, filter.toLowerCase(), modData.slug, selectedInstance.getGameVersion(), false);
        }

        public void setData(ModData modData) {
            State.Instance selectedInstance = ModManager.state.getInstance("Default");
            ModData installedMod = selectedInstance.getMod(modData.slug);
            if (installedMod != null) modData = installedMod; //Check if mod is already installed and overwrite fetched data

            this.modData = modData;
            title.setText(modData.title);
            if (modData.iconUrl != null && !modData.iconUrl.isEmpty()) Picasso.get().load(modData.iconUrl).into(icon);
            enableSwitch.setChecked(modData.isActive);

            String name = modData.slug;
            if (filter.equals("CurseForge")) name = modData.title;
            String modCompat = ModManager.getModCompat(modData.platform, name);

            if (modCompat.equals("Untested")) compat.setBackgroundResource(R.drawable.marker_gray);
            if (modCompat.equals("Perfect")) compat.setBackgroundResource(R.drawable.marker_green);
            if (modCompat.equals("Good")) compat.setBackgroundResource(R.drawable.marker_yellow);
            if (modCompat.equals("Unusable")) compat.setBackgroundResource(R.drawable.marker_red);
            if (filter.equals("Core")) {
                compat.setBackgroundResource(R.drawable.marker_green);
                modCompat = "Core";
            }

            compat.setText("  " + modCompat + "  ");
        }

        @Override
        public void onClick(View view) {
            adapter.selectViewHolder(this);
            adapter.loadProjectPage(modData, icon);
        }

        public void doMarquee(boolean value) {
            title.setSelected(value);
        }

        /*@Override
        public boolean onLongClick(View view) {
            ModData installedMod = ModManager.getMod("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2", modData.slug);
            if (installedMod == null) return false;

            ModManager.removeMod("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2", modData.slug);
            return false;
        }*/
    }

    public static class ModAdapter extends RecyclerView.Adapter<ModViewHolder> {

        private final ModsFragment fragment;
        private final ArrayList<ModData> mods = new ArrayList<>();
        private ModViewHolder selectedHolder;

        public ModAdapter(ModsFragment fragment) {
            this.fragment = fragment;
        }

        public void addMods(ArrayList<ModData> newMods) {
            int startPos = mods.size();
            mods.addAll(newMods);
            this.notifyItemRangeChanged(startPos, mods.size());
        }

        public void reset() {
            mods.clear();
            this.notifyDataSetChanged();
        }

        public void selectViewHolder(ModViewHolder holder) {
            if (selectedHolder != null) selectedHolder.doMarquee(false);
            selectedHolder = holder;
            holder.doMarquee(true);
        }

        public int getOffset() {
            return mods.size();
        }

        public void loadProjectPage(ModData modData, ImageView icon) {
            View view = this.fragment.getView();
            if (view == null) return;

            ImageView iconMain = view.findViewById(R.id.mod_icon_main);
            TextView titleMain = view.findViewById(R.id.mod_title_main);
            MarkdownView bodyMain = view.findViewById(R.id.mod_description);
            titleMain.setText(modData.title);

            if (modData.platform.equals("modrinth")) Modrinth.loadProjectPage(bodyMain, modData.slug);
            else if (modData.platform.equals("curseforge")) Curseforge.loadProjectPage(bodyMain, modData.slug);
            else if (modData.platform.equals("github")) Github.loadProjectPage(bodyMain, modData.repo);

            if (icon != null) iconMain.setImageDrawable(icon.getDrawable());
            else Picasso.get().load(modData.iconUrl).into(iconMain);
        }

        @Override
        public int getItemViewType(final int position) {
            return R.layout.item_mod;
        }

        @NonNull
        @Override
        public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new ModViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
            if (mods.size() > position) {
                holder.enableSwitch.setOnCheckedChangeListener(null);
                holder.setData(mods.get(position));
                holder.enableSwitch.setOnCheckedChangeListener((compoundButton, b) -> holder.installMod());
            }
        }

        @Override
        public int getItemCount() {
            return mods.size();
        }
    }
}