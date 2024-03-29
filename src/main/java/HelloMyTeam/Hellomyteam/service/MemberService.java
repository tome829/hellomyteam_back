package HelloMyTeam.Hellomyteam.service;


import HelloMyTeam.Hellomyteam.dto.TeamMemberIdDto;
import HelloMyTeam.Hellomyteam.dto.TeamDto;
import HelloMyTeam.Hellomyteam.entity.Member;
import HelloMyTeam.Hellomyteam.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    public Member findMemberByTeamInfo(TeamDto teamInfo) {
        Member member = memberRepository.findById(teamInfo.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("memberId가 누락되었습니다."));
        return member;
    }

    public Member findMemberByTeamMemberId(TeamMemberIdDto memberIdParam) {
        Member member = memberRepository.findById(memberIdParam.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("memberId가 누락되었습니다."));
        return member;
    }
}
