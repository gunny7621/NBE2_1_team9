package team9.gccoffee.domain.member.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team9.gccoffee.domain.member.domain.Member;
import team9.gccoffee.domain.member.domain.MemberType;
import team9.gccoffee.domain.member.dto.MemberRequestDTO;
import team9.gccoffee.domain.member.dto.MemberResponseDTO;
import team9.gccoffee.domain.member.dto.MemberUpdateDTO;
import team9.gccoffee.domain.member.dto.MemberPageRequestDTO;
import team9.gccoffee.domain.member.repository.MemberRepository;

import java.util.Optional;
import team9.gccoffee.domain.order.domain.Order;
import team9.gccoffee.global.exception.MemberException;
import team9.gccoffee.global.exception.MemberTaskException;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    ///////
    //조회
    //멤버 개인 조회
    @Override
    public  MemberResponseDTO getMemberById(Long memberId) {
        Optional<Member> foundMember = memberRepository.findById(memberId);

        Member member = foundMember.orElseThrow(MemberException.NOT_FOUND::get);

        return new MemberResponseDTO(member);
    }

    //멤버 전체 조회
    @Override
    public Page<Member> getAllMembers(MemberPageRequestDTO memberPageRequestDTO, Long memberId) {
        Optional<Member> foundMember = memberRepository.findById(memberId);
        Member member = foundMember.orElseThrow(MemberException.NOT_FOUND::get);

        //해당 member 의 memberType 체크 하여 관리자인 경우 전체 조회 가능
        if (member.getMemberType() == MemberType.ADMIN) {
            Sort sort = Sort.by("memberId").descending();

            Pageable pageable = memberPageRequestDTO.getPageable(sort);
            return memberRepository.findAll(pageable);
       } else {
            throw MemberException.ACCESS_DENIED.get();
        }

    }

    //개인 주문 조회
    @Transactional(readOnly = true)
    public List<Order> getOrdersForMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));

        return member.getOrderList();
        //orderList 반환
    }


    //////////

    //수정
    @Override
    public MemberResponseDTO updateMember(MemberUpdateDTO memberUpdateDTO) {
        Optional<Member> foundMember
                = memberRepository.findById(memberUpdateDTO.getId());

        Member member = foundMember.orElseThrow(MemberException.NOT_FOUND::get);
        try {
            member.changeName(memberUpdateDTO.getName());
            member.changeEmail(memberUpdateDTO.getEmail());
            member.changePostcode(memberUpdateDTO.getPostcode());
            member.changeAddress(memberUpdateDTO.getAddress());
            //member = memberUpdateDTO.toEntity();

            return new MemberResponseDTO(member);
        } catch (Exception e){
            log.error("--- " + e.getMessage());
            throw MemberException.NOT_MODIFIED.get();
        }

    }


    //삭제
    @Override
    public void deleteMember(Long memberId) {
        Optional<Member> foundMember = memberRepository.findById(memberId);
        Member member = foundMember.orElseThrow(MemberException.NOT_FOUND::get);

        try {
            memberRepository.delete(member);
        } catch (Exception e) {
            throw MemberException.NOT_REMOVED.get();
        }
    }


    //등록
    @Override
    public MemberResponseDTO createMember(MemberRequestDTO memberRequestDTO) {
        if (memberRequestDTO.getMemberType() == MemberType.ADMIN) {
            if (!"ADMIN000".equals(memberRequestDTO.getAdminCode())) {
                throw new SecurityException("Invalid admin code!!");
            }
        }
        try {
            Member member = memberRequestDTO.toEntity();
            memberRepository.save(member);
            return new MemberResponseDTO(member);
        } catch (Exception e) {
            throw MemberException.NOT_REGISTERED.get();
        }

    }
}
