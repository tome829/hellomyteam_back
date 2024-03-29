package HelloMyTeam.Hellomyteam.api;

import HelloMyTeam.Hellomyteam.dto.*;
import HelloMyTeam.Hellomyteam.entity.Image;
import HelloMyTeam.Hellomyteam.entity.Member;
import HelloMyTeam.Hellomyteam.entity.Team;
import HelloMyTeam.Hellomyteam.entity.TeamMemberInfo;
import HelloMyTeam.Hellomyteam.dto.CommonResponse;
import HelloMyTeam.Hellomyteam.service.MemberService;
import HelloMyTeam.Hellomyteam.service.TeamService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;
    private final MemberService memberService;

    @ApiOperation(value = "팀 정보 가져오기", notes = "팀 엔티티에 대한 정보를 가져온다.")
    @GetMapping("/{teamId}")
    public CommonResponse<?> getTeamInfo(@PathVariable Long teamId) {
        Team team = teamService.findTeamById(teamId);
        return CommonResponse.createSuccess(team, "team 정보 전달");
    }

    @ApiOperation(value = "팀 생성", notes = "팀 생성: 회원테이블에 member_id가 존재해야한다.")
    @PostMapping("/create")
    public CommonResponse<?> saveTeam(@RequestBody TeamDto teamInfo) {
        Team team = teamService.createTeamWithAuthNo(teamInfo);
        Member member = memberService.findMemberByTeamInfo(teamInfo);
        teamService.teamMemberInfoSaveAuthLeader(team, member);
        return CommonResponse.createSuccess(team);
    }

    @ApiOperation(value = "팀 찾기", notes = "팀 이름 혹은 팀 고유번호를 입력해야한다. ")
    @GetMapping("/find")
    public CommonResponse<?> findTeam (@RequestParam(value = "teamName", required = false) String teamName,
                                       @RequestParam(value = "teamSerialNo", required = false) Integer teamSerialNo) {
        List<TeamSearchDto> findTeams = teamService.findTeamBySearchCond(teamName, teamSerialNo);

        if (findTeams.isEmpty()) {
            return CommonResponse.createSuccess(findTeams, "검색 결과가 없습니다.");
        }
        return CommonResponse.createSuccess(findTeams, "검색 결과 success");
    }


    @ApiOperation(value = "팀 가입신청", notes = "team_id를 통한 팀 가입신청, 가입 수락 전 권한 = WAIT")
    @PostMapping("/join")
    public CommonResponse<?> joinTeam(@RequestBody TeamMemberIdDto teamMemberIdParam) {
        Member member = memberService.findMemberByTeamMemberId(teamMemberIdParam);
        Team team = teamService.findTeamByTeamMemberId(teamMemberIdParam);
        TeamMemberInfo result = teamService.joinTeamAuthWait(team, member);
        return CommonResponse.createSuccess(result, "null일 경우 중복가입 체크, 리더 본인팀 가입 x");
    }

    @ApiOperation(value = "팀 가입신청자 정보 가져오기", notes = "알림페이지에 띄어질 정보, 가입 수락 전 권한 = WAIT")
    @GetMapping("/{teamId}/join")
    public CommonResponse<?> getApplicant(@PathVariable Long teamId) {
        List<ApplicantDto> applicantDto = teamService.findAppliedTeamMember(teamId);
        return CommonResponse.createSuccess(applicantDto, "팀 가입 신청한 회원 정보 가져오기");
    }

    @ApiOperation(value = "팀원 수락", notes = "팀 가입 신청에 따른 팀원 수락, 가입할 memberId와, 가입할 teamId 입력")
    @PostMapping("/{teamId}/member/accept")
    public CommonResponse<?> acceptTeamMember(@PathVariable Long teamId, @RequestBody MemberIdDto memberIdDto) {
        int changeNo = teamService.acceptTeamMemberById(teamId, memberIdDto);

        String stringResult = Integer.toString(changeNo);
        String template = "총 %s 명이 반영되었습니다.";
        String message = String.format(template, stringResult);
        return CommonResponse.createSuccess(true, message);
    }

    @ApiOperation(value = "팀원 수락 거절")
    @PostMapping(value = "/{teamId}/member/reject")
    public CommonResponse<?> rejectMember(@PathVariable Long teamId, @RequestBody MemberIdDto memberIdParam) {
        Long count = teamService.deleteMemberByMemberId(teamId, memberIdParam);
        if (count == 0) {
            return CommonResponse.createSuccess(0, "선택 된 팀원이 없으므로 API 결과값이 0 입니다.");
        }
        String stringResult = Long.toString(count);
        String template = "총 %s 명이 수락 거절 되었습니다.";
        String message = String.format(template, stringResult);
        return CommonResponse.createSuccess(count, message);
    }

    @ApiOperation(value = "팀 로고 단일 추가 및 존재시 업데이트",
            notes = "해당 API는 포스트맨에서 진행할 것, " +
                    "KEY: imgFile, VALUE: 이미지파일 / KEY: teamIdParam, VALUE: {\"teamId\": 숫자})" +
                    "참고 링크: https://smooth-foxtrot-e11.notion.site/swagger-mine-6f0e4ca8ad964f56b0c23d86d6780b98"
    )
    @PostMapping(value = "/{teamId}/logo", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public CommonResponse<?> updateLogo(@PathVariable Long teamId, @RequestPart MultipartFile imgFile) throws IOException {
        List<Image> savedImage = teamService.saveLogo(imgFile, teamId);
        return CommonResponse.createSuccess(savedImage, "팀 로고 등록 success <type:List>");
    }

    @ApiOperation(value = "팀 로고 삭제")
    @PostMapping(value = "/{teamId}/logo/delete")
    public CommonResponse<?> deleteLogo(@PathVariable Long teamId) {
        List<Image> image = teamService.deleteLogoByTeamId(teamId);
        return CommonResponse.createSuccess(image, "팀 로고 삭제 success");
    }


    @ApiOperation(value = "팀 탈퇴")
    @PostMapping(value = "/{teamId}/withDraw")
    public CommonResponse<?> withDrawTeam(@PathVariable Long teamId, @RequestBody MemberIdDto memberIdParam) {
        Map<String, String> param = teamService.withDrawTeamByMemberId(teamId, memberIdParam);
        return CommonResponse.createSuccess(param.get("authorityStatus"), param.get("message"));
    }

    @ApiOperation(value = "팀원 리스트 정보 가져오기", notes = "team_id를 통한 팀원 리스트 가져오기")
    @GetMapping(value = "/{teamId}/members")
    public CommonResponse<?> getTeamMemberInfos(@PathVariable Long teamId) {
        List<TeamMemberInfosResDto> teamMemberInfosResDtos = teamService.getTeamMemberInfos(teamId);
        return CommonResponse.createSuccess(teamMemberInfosResDtos, "팀원 정보 가져오기 success");
    }

    @ApiOperation(value = "팀원 상세 정보 가져오기")
    @GetMapping(value = "/member/{teamMemberInfoId}")
    public CommonResponse<?> getTeamMemberInfo(@PathVariable Long teamMemberInfoId) {
        TeamMemberInfoDto teamMemberInfoDto = teamService.getTeamMemberInfo(teamMemberInfoId);
        return CommonResponse.createSuccess(teamMemberInfoDto, "팀원 정보 가져오기 success");
    }

    @ApiOperation(value = "팀원 상세 정보 수정", notes = "본인일 경우에만 수정 가능")
    @PutMapping(value = "/member/{teamMemberInfoId}")
    public CommonResponse<?> editTeamMemberInfo(@PathVariable Long teamMemberInfoId,
                                                @RequestBody TeamInfoUpdateDto teamInfoUpdateDto
    ) {
        TeamMemberInfoDto teamMemberInfoDto = teamService.editTeamMemberInfo(teamMemberInfoId, teamInfoUpdateDto);
        return CommonResponse.createSuccess(teamMemberInfoDto, "내 정보 수정 success");
    }

    //TODO 팀 관리/정보 수정
//    @ApiOperation(value = "팀 관리 / 정보 수정")
//    @PutMapping(value = "/member/{memberId}")
//    public CommonResponse<?> editTeamMemberInfo(@PathVariable(value = "memberId") Long memberId,
//                                                @RequestParam(required = true, value = "teamId") Long teamId,
//                                                @RequestBody TeamInfoUpdateDto teamInfoUpdateDto
//    ) {
//        TeamMemberInfoDto teamMemberInfoDto = teamService.editTeamMemberInfo(teamInfoUpdateDto, memberId, teamId);
//        return CommonResponse.createSuccess(teamMemberInfoDto, "내 정보 수정 success");
//    }

    //TODO 팀원 프로필 등록

}
