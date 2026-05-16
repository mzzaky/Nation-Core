package id.nationcore.managers;

import id.nationcore.NationCore;
import id.nationcore.models.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.UUID;

public class DiplomacyManager {

    private final NationCore plugin;

    public DiplomacyManager(NationCore plugin) {
        this.plugin = plugin;
    }

    public boolean canManageDiplomacy(UUID playerUUID, Nation nation) {
        if (nation == null) return false;

        if (nation.getLeaderUUID().equals(playerUUID)) {
            return true;
        }

        if (nation.getType() == GovernmentType.REPUBLIC) {
            Government gov = nation.getRepublicGovernment();
            if (gov != null) {
                UUID modUUID = gov.getCabinetMember(Government.CabinetPosition.DEFENSE);
                return playerUUID.equals(modUUID);
            }
        } else if (nation.getType() == GovernmentType.COMMUNIST) {
            CommunistGovernment gov = nation.getCommunistGovernment();
            if (gov != null) {
                CommunistGovernment.PolitburoMember mod = gov.getPolitburoMember(CommunistGovernment.PolitburoPosition.DEFENSE);
                if (mod != null && mod.getUuid().equals(playerUUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void proposeDiplomacy(Player sender, Nation senderNation, Nation targetNation, DiplomacyStatus targetStatus) {
        if (senderNation.getId().equals(targetNation.getId())) {
            sender.sendMessage(ChatColor.RED + "Anda tidak bisa mengajukan diplomasi ke nation sendiri.");
            return;
        }

        if (!canManageDiplomacy(sender.getUniqueId(), senderNation)) {
            sender.sendMessage(ChatColor.RED + "Hanya Pemimpin dan Menteri Pertahanan yang bisa mengajukan diplomasi.");
            return;
        }

        DiplomacyStatus currentStatus = senderNation.getDiplomacyStatusWith(targetNation.getId());
        if (currentStatus == targetStatus) {
            sender.sendMessage(ChatColor.RED + "Status diplomasi dengan " + targetNation.getName() + " sudah " + targetStatus.getDisplayName() + ".");
            return;
        }

        // Cek jika sudah ada request yang menggantung
        DiplomacyRequest existingRequest = targetNation.getDiplomacyRequest(senderNation.getId());
        if (existingRequest != null) {
            sender.sendMessage(ChatColor.RED + "Nation Anda sudah mengajukan diplomasi ke " + targetNation.getName() + ". Menunggu respon.");
            return;
        }

        DiplomacyRequest newRequest = new DiplomacyRequest(
                senderNation.getId(),
                targetNation.getId(),
                targetStatus,
                sender.getUniqueId()
        );

        targetNation.addDiplomacyRequest(newRequest);

        sender.sendMessage(ChatColor.GREEN + "Berhasil mengajukan status " + targetStatus.getColoredName() + ChatColor.GREEN + " kepada " + targetNation.getName() + ".");

        // Broadcast to target nation's leaders (implikasi)
        Player targetLeader = plugin.getServer().getPlayer(targetNation.getLeaderUUID());
        if (targetLeader != null && targetLeader.isOnline()) {
            targetLeader.sendMessage(ChatColor.YELLOW + "[DIPLOMASI] " + senderNation.getName() + " mengajukan status " + targetStatus.getColoredName() + ChatColor.YELLOW + ". Gunakan menu diplomasi untuk merespon.");
        }
    }

    public void acceptDiplomacy(Player responder, Nation targetNation, String senderNationId) {
        if (!canManageDiplomacy(responder.getUniqueId(), targetNation)) {
            responder.sendMessage(ChatColor.RED + "Hanya Pemimpin dan Menteri Pertahanan yang bisa merespon diplomasi.");
            return;
        }

        DiplomacyRequest request = targetNation.getDiplomacyRequest(senderNationId);
        if (request == null) {
            responder.sendMessage(ChatColor.RED + "Tidak ada pengajuan diplomasi dari nation tersebut.");
            return;
        }

        NationManager nationManager = plugin.getNationManager(); // Asumsi ada get properties ini
        Nation senderNation = nationManager.getNation(senderNationId);

        if (senderNation != null) {
            DiplomacyStatus status = request.getRequestedStatus();
            
            // Set two-way
            senderNation.setDiplomacyStatus(targetNation.getId(), status);
            targetNation.setDiplomacyStatus(senderNation.getId(), status);

            responder.sendMessage(ChatColor.GREEN + "Anda menyetujui pengajuan diplomasi. Status dengan " + senderNation.getName() + " sekarang adalah " + status.getColoredName() + ChatColor.GREEN + ".");
            
            Player senderPlayer = plugin.getServer().getPlayer(request.getRequestedBy());
            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(ChatColor.GREEN + "[DIPLOMASI] " + targetNation.getName() + " MENYETUJUI pengajuan diplomasi. Status sekarang: " + status.getColoredName());
            }
        } else {
            responder.sendMessage(ChatColor.RED + "Nation pengaju tidak ditemukan.");
        }

        targetNation.removeDiplomacyRequest(senderNationId);
    }

    public void rejectDiplomacy(Player responder, Nation targetNation, String senderNationId) {
        if (!canManageDiplomacy(responder.getUniqueId(), targetNation)) {
            responder.sendMessage(ChatColor.RED + "Hanya Pemimpin dan Menteri Pertahanan yang bisa merespon diplomasi.");
            return;
        }

        DiplomacyRequest request = targetNation.getDiplomacyRequest(senderNationId);
        if (request == null) {
            responder.sendMessage(ChatColor.RED + "Tidak ada pengajuan diplomasi dari nation tersebut.");
            return;
        }

        NationManager nationManager = plugin.getNationManager();
        Nation senderNation = nationManager.getNation(senderNationId);

        if (senderNation != null) {
            responder.sendMessage(ChatColor.YELLOW + "Anda MENOLAK pengajuan diplomasi dari " + senderNation.getName() + ".");
            
            Player senderPlayer = plugin.getServer().getPlayer(request.getRequestedBy());
            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(ChatColor.RED + "[DIPLOMASI] " + targetNation.getName() + " MENOLAK pengajuan diplomasi Anda.");
            }
        }

        targetNation.removeDiplomacyRequest(senderNationId);
    }
}
